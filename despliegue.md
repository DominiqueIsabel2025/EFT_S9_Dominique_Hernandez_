# Despliegue en la nube (AWS EC2 + Docker Compose)

El mismo `docker-compose.yml` que se usa en local se ejecuta en una instancia **EC2**. Es la opción más simple para esta evaluación.
Los 14 contenedores usan unos 5–6 GB de RAM, por eso se recomienda una instancia **t3.xlarge (16 GB)**. Con `t3.large` (8 GB) puede funcionar pero queda justo.

## 1. Crear la instancia
1. Consola AWS → **EC2 → Launch instance**.
2. AMI: **Amazon Linux 2023** · Tipo: **t3.xlarge** · Disco: **30 GB gp3** · Key pair: crear o elegir uno (`.pem`).
3. **Security Group** (inbound):

| Puerto | Origen | Para qué |
|---|---|---|
| 22 | Tu IP | SSH |
| 8080 | Tu IP | API Gateway (entrada de negocio y batch) |
| 8081, 8082, 8083 | Tu IP | BFF Web, Móvil, Cajero |
| 9000 | Tu IP | Auth Server (tokens) |
| 8761, 8888, 8090, 8084 | Tu IP | Paneles: Eureka, Config, Kafka UI, Notificaciones |

> No abras **29092** (Kafka) ni los puertos internos 8101–8103 a internet. Usa "Tu IP" y no `0.0.0.0/0`, salvo unos minutos para la demo.

4. (Opcional) En *Advanced details → User data* pega `aws/ec2-user-data.sh` para instalar Docker automáticamente.

## 2. Instalar Docker (si no usaste user-data)
```bash
ssh -i mi-llave.pem ec2-user@<IP_PUBLICA>
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64 \
     -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
exit    # volver a entrar por SSH para aplicar el grupo docker
```

## 3. Desplegar
```bash
git clone https://github.com/DominiqueIsabel2025/EFT_S9_Dominique_Hernandez_.git
cd EFT_S9_Dominique_Hernandez_
cp .env.example .env
nano .env        # cambia los secretos y pon KAFKA_EXTERNAL_HOST=<IP_PUBLICA>
docker compose up -d --build
docker compose ps        # esperar a que todos estén "healthy" (3-5 min)
```
Probar desde tu PC: en Postman, en el environment, cambia `localhost` por `http://<IP_PUBLICA>` en las URLs (`core_url`, `web_url`, `mobile_url`, `atm_url`, `notif_url`, `auth_url`).
El claim `iss` de los tokens es fijo (`http://auth-server:9000`), por lo que **no hay que cambiar nada más**.

## 4. Escalar horizontalmente
```bash
docker compose up -d --scale clientes-service=3
```
Eureka (`http://<IP_PUBLICA>:8761`) mostrará 3 instancias y el API Gateway reparte las llamadas entre ellas.
> Cuentas y Pagos usan H2 en memoria: para escalarlos hay que migrar a una base compartida (RDS, sección 6).

## 5. (Opcional) Publicar las imágenes en Amazon ECR
```bash
REGION=us-east-1
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ACCOUNT.dkr.ecr.$REGION.amazonaws.com
for svc in auth-server eureka-server config-server api-gateway clientes-service cuentas-service pagos-service \
           batch-service notification-service bff-web-service bff-mobile-service bff-atm-service; do
  aws ecr create-repository --repository-name banco-xyz/$svc --region $REGION || true
  docker tag banco-xyz/$svc:1.0.0 $ACCOUNT.dkr.ecr.$REGION.amazonaws.com/banco-xyz/$svc:1.0.0
  docker push $ACCOUNT.dkr.ecr.$REGION.amazonaws.com/banco-xyz/$svc:1.0.0
done
```
(Las imágenes existen tras ejecutar `docker compose build`.)

## 6. Evolución recomendada en AWS
| Hoy (EC2 + compose) | Equivalente administrado |
|---|---|
| Contenedores en una EC2 | **ECS Fargate** (con imágenes de ECR) |
| Kafka en contenedor | **Amazon MSK** |
| H2 en memoria | **Amazon RDS (PostgreSQL)** por servicio |
| Secretos en `.env` | **AWS Secrets Manager / SSM Parameter Store** |
| Puertos públicos HTTP | **Application Load Balancer + HTTPS** (certificado en ACM) → cumple "comunicaciones seguras (HTTPS)" |
| Config Server *native* | Config Server con repositorio Git |
| Archivos CSV del batch en volumen | **Amazon S3** (entrada y salida de los jobs) |

## 7. Limpieza (evitar costos)
```bash
docker compose down -v
```
y luego **Terminate instance** en la consola de EC2.
