# FTP a S3 Transfer

Aplicación en Java 21 que permite ejecutar de forma recurrente (por ejemplo, desde `cron`) la copia de archivos disponibles en un servidor SFTP hacia un bucket de Amazon S3. El proceso registra métricas básicas, continúa procesando archivos aun cuando alguno falle y permite eliminar el archivo original cuando la transferencia fue exitosa.

## Requisitos

* Java 21
* Maven 3.9+
* Credenciales de AWS configuradas (variables de entorno, perfil local o IAM role)

## Construcción

```bash
mvn clean package
```

El comando anterior genera un **fat JAR** en `target/ftp-to-s3-transfer-1.0.0-shaded.jar`.

## Configuración

La aplicación se configura mediante un archivo `.properties`. Puede basarse en `config/sample.properties`:

```properties
sftp.host=sftp.example.com
sftp.port=22
sftp.username=usuario
sftp.password=secreto
sftp.remoteDirectory=/ruta/remota
s3.bucket=mi-bucket
s3.prefix=ftp-backup
transfer.deleteAfterUpload=false
```

También es posible autenticarse mediante llave privada (`sftp.privateKeyPath`), definir un `known_hosts` y especificar la región de S3 (`s3.region`).

## Ejecución

```bash
java -jar target/ftp-to-s3-transfer-1.0.0-shaded.jar --config=/ruta/a/config.properties
```

### Uso desde `cron`

Ejemplo de entrada en `crontab` que ejecuta el proceso cada 15 minutos y redirige la salida a un log:

```cron
*/15 * * * * /usr/bin/java -jar /opt/ftp-to-s3-transfer.jar --config=/opt/conf/transfer.properties >> /var/log/ftp-to-s3-transfer.log 2>&1
```

El proceso finaliza con código `0` si todas las transferencias fueron exitosas, `2` si algún archivo falló y `1` ante un error general (por ejemplo, de configuración). Esto permite instrumentar alarmas basadas en el estado de salida.

## Métricas y observabilidad

* Se registran mensajes en formato legible que indican el inicio y fin de la ejecución, archivos transferidos y tiempos por archivo.
* Si una transferencia falla, se continúa con el siguiente archivo y se contabiliza el error.
* Al finalizar se imprime un resumen con totales y duración.

Estos logs pueden integrarse con sistemas de observabilidad (CloudWatch Logs, ELK, etc.) para generar alarmas en caso de fallas reiteradas.
