# Streaming de certificados: Kinesis + Firehose

## Flujo

```
Angular ──(cookie de sesión)──► BFF :8084 ──(JWT Cognito)──► API Manager :8080
                                                                │
              POST /api/cursos/codigo/{codigo}/certificado ◄────┘
              ms-cursos :8082  (valida inscripción, marca completado)
                     │
                     ▼
   Kinesis Data Stream  certifika2-certificados-solicitados  (2 shards, partition key = sub del usuario)
          │                                          │
          │                                          ▼
          │                   Firehose certifika2-solicitudes-archivo ─► S3 analytics/solicitudes/
          ▼
   ms-usuarios :8081 (consumidor con checkpoint por shard)
          │ genera PDF/PNG ─► S3 certifika2-certificados/certificados/{sub}/{idCurso}/...
          │ guarda Certificado (sub + FK usuario)
          ├─► Firehose certifika2-certificados-emitidos ─► S3 analytics/emitidos/
          └─► (falla) Kinesis certifika2-certificados-dlq
```

## Levantar en local (LocalStack)

```bash
docker compose -f infra/docker-compose.yml up -d
docker logs -f certifika2-localstack     # esperar "[init] Recursos creados"
```

ms-cursos y ms-usuarios usan `AWS_ENDPOINT=http://localhost:4566` por defecto.
Para AWS real: `AWS_ENDPOINT=` (vacío) y credenciales por variables de entorno o rol de la instancia.

## Desplegar en AWS real

```bash
aws cloudformation deploy \
  --template-file infra/cloudformation/certificados-streaming.yaml \
  --stack-name certifika2-streaming \
  --capabilities CAPABILITY_IAM
# AWS Academy (sin permisos IAM): agregar
#   --parameter-overrides FirehoseRoleArn=arn:aws:iam::<cuenta>:role/LabRole
```

En AWS real los buckets llevan sufijo con el id de cuenta: configurar
`S3_BUCKET_CERTIFICADOS=certifika2-certificados-<cuenta>` en ms-usuarios.

## Variables de entorno

| Servicio | Variable | Default |
|---|---|---|
| ambos | `AWS_REGION` | `us-east-1` |
| ambos | `AWS_ENDPOINT` | `http://localhost:4566` (vacío = AWS real) |
| ambos | `KINESIS_STREAM_SOLICITUDES` | `certifika2-certificados-solicitados` |
| ms-usuarios | `KINESIS_STREAM_DLQ` | `certifika2-certificados-dlq` |
| ms-usuarios | `KINESIS_CONSUMER_ENABLED` | `true` |
| ms-usuarios | `KINESIS_CONSUMER_NAME` | `ms-usuarios-certificados` |
| ms-usuarios | `FIREHOSE_STREAM_EMITIDOS` | `certifika2-certificados-emitidos` |
| ms-usuarios | `S3_BUCKET_CERTIFICADOS` | `certifika2-certificados` |
| api-manager | `CERTIFICADOS_SERVICE_URL` | `http://localhost:8081/api/certificados` |

## Prueba directa contra los microservicios

```bash
TOKEN=<access token de Cognito>
curl -X POST -H "Authorization: Bearer $TOKEN" localhost:8082/api/cursos/codigo/cloud-native/inscribir
curl -X POST -H "Authorization: Bearer $TOKEN" "localhost:8082/api/cursos/codigo/cloud-native/certificado?formato=PDF"

# Ver el evento en Kinesis
SHARD=$(awslocal kinesis list-shards --stream-name certifika2-certificados-solicitados --query 'Shards[0].ShardId' --output text)
IT=$(awslocal kinesis get-shard-iterator --stream-name certifika2-certificados-solicitados --shard-id $SHARD --shard-iterator-type TRIM_HORIZON --query ShardIterator --output text)
awslocal kinesis get-records --shard-iterator $IT

# Consultar y descargar
curl -H "Authorization: Bearer $TOKEN" localhost:8081/api/certificados
curl -H "Authorization: Bearer $TOKEN" -o certificado.pdf localhost:8081/api/certificados/1/descarga
curl localhost:8081/api/certificados/verificar/<codigoVerificacion>

# Archivos en S3 (Firehose entrega cada ~60 s)
awslocal s3 ls s3://certifika2-certificados --recursive
awslocal s3 ls s3://certifika2-analytics --recursive

# Probar la DLQ con un mensaje inválido
awslocal kinesis put-record --stream-name certifika2-certificados-solicitados \
  --partition-key test --data "$(echo -n 'no-es-json' | base64)"
```

(`awslocal` = `docker exec certifika2-localstack awslocal` si no lo tienes instalado.)

## Decisiones

| Parámetro | Valor | Motivo |
|---|---|---|
| Shards | 2 | 1 shard admite 1.000 registros/s de escritura; 2 dan paralelismo entre usuarios con margen y costo bajo. |
| Partition key | `sub` del usuario | Orden garantizado por usuario; usuarios distintos se reparten entre shards. |
| Retención | 48 h | Permite reprocesar si el consumidor estuvo caído un fin de semana. |
| Checkpoint | Tabla `kinesis_checkpoints` | Reanuda tras reinicios; semántica al-menos-una-vez. |
| Idempotencia | `eventId` único y (sub, curso, formato) único | Un reintento o duplicado nunca genera dos certificados. |
| Reintentos | 3, backoff 500/1000 ms | Para fallas transitorias (S3, BD). Fallas definitivas (JSON inválido, contrato no soportado) van directo a la DLQ. |
| Firehose buffer | 1–5 MB / 60 s | Pocos archivos grandes en S3 en vez de miles de archivos pequeños. |

**Límite conocido:** el consumidor lee con `GetRecords` y checkpoint propio, pensado para
una sola instancia de ms-usuarios. Para escalar a varias instancias conviene migrar a la
Kinesis Client Library (KCL), que reparte shards entre instancias con leases en DynamoDB.
