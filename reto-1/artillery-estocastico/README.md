# 🚀 Instalación y Ejecución en AWS (EC2)

## 📌 Descripción

Este proyecto ejecuta pruebas de carga utilizando **Artillery** desde una instancia **EC2 en AWS** contra un API definido en el archivo `test.yml`.

La ejecución se realiza desde una máquina virtual en la nube, permitiendo simular tráfico externo hacia el servicio que se desea evaluar.

---

# 🖥 Creación de la instancia EC2

## Configuración mínima recomendada

- **AMI:** Amazon Linux 2023  
- **Tipo de instancia:** `t3.micro`  
- **Almacenamiento:** 8 GB  

## Instalar artillery

sudo npm install -g artillery

## Ejecutar el test

artillery run test.yml -o resultado.json
artillery report resultado.json

