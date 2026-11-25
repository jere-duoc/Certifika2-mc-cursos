#!/bin/bash
# Se ejecuta automáticamente cuando LocalStack está listo (ready.d).
# Crea: 2 streams Kinesis, 2 buckets S3, rol IAM y 2 delivery streams de Firehose.
set -euo pipefail

REGION=us-east-1
ACCOUNT=000000000000
STREAM=certifika2-certificados-solicitados
DLQ_STREAM=certifika2-certificados-dlq
BUCKET_CERTS=certifika2-certificados
BUCKET_ANALYTICS=certifika2-analytics
ROLE=certifika2-firehose-role

echo "[init] Creando streams Kinesis..."
# 2 shards: el partition key es el sub del usuario, así que las solicitudes de un mismo
# usuario quedan ordenadas en un shard y usuarios distintos se procesan en paralelo.
awslocal kinesis create-stream --stream-name "$STREAM" --shard-count 2 || true
awslocal kinesis create-stream --stream-name "$DLQ_STREAM" --shard-count 1 || true
awslocal kinesis wait stream-exists --stream-name "$STREAM"
awslocal kinesis wait stream-exists --stream-name "$DLQ_STREAM"
# Retención de 48 h para poder reprocesar si el consumidor estuvo caído un fin de semana.
awslocal kinesis increase-stream-retention-period --stream-name "$STREAM" --retention-period-hours 48 || true

echo "[init] Creando buckets S3..."
awslocal s3 mb "s3://$BUCKET_CERTS" || true
awslocal s3 mb "s3://$BUCKET_ANALYTICS" || true

echo "[init] Creando rol IAM para Firehose..."
awslocal iam create-role --role-name "$ROLE" \
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"firehose.amazonaws.com"},"Action":"sts:AssumeRole"}]}' || true

ROLE_ARN="arn:aws:iam::$ACCOUNT:role/$ROLE"
STREAM_ARN="arn:aws:kinesis:$REGION:$ACCOUNT:stream/$STREAM"

echo "[init] Firehose 1: archivo de solicitudes (fuente = Kinesis Data Stream)"
awslocal firehose create-delivery-stream \
  --delivery-stream-name certifika2-solicitudes-archivo \
  --delivery-stream-type KinesisStreamAsSource \
  --kinesis-stream-source-configuration "KinesisStreamARN=$STREAM_ARN,RoleARN=$ROLE_ARN" \
  --extended-s3-destination-configuration "{
    \"RoleARN\": \"$ROLE_ARN\",
    \"BucketARN\": \"arn:aws:s3:::$BUCKET_ANALYTICS\",
    \"Prefix\": \"solicitudes/\",
    \"ErrorOutputPrefix\": \"errores/solicitudes/\",
    \"BufferingHints\": {\"SizeInMBs\": 1, \"IntervalInSeconds\": 60},
    \"CompressionFormat\": \"UNCOMPRESSED\"
  }" || true

echo "[init] Firehose 2: certificados emitidos (DirectPut desde ms-usuarios)"
awslocal firehose create-delivery-stream \
  --delivery-stream-name certifika2-certificados-emitidos \
  --delivery-stream-type DirectPut \
  --extended-s3-destination-configuration "{
    \"RoleARN\": \"$ROLE_ARN\",
    \"BucketARN\": \"arn:aws:s3:::$BUCKET_ANALYTICS\",
    \"Prefix\": \"emitidos/\",
    \"ErrorOutputPrefix\": \"errores/emitidos/\",
    \"BufferingHints\": {\"SizeInMBs\": 1, \"IntervalInSeconds\": 60},
    \"CompressionFormat\": \"UNCOMPRESSED\"
  }" || true

echo "[init] Recursos creados:"
awslocal kinesis list-streams
awslocal firehose list-delivery-streams
awslocal s3 ls
