
# maven-example



## In-toto 

```shell
openssl genrsa -out signer.pem 2048
openssl rsa -in signer.pem -pubout -out signer.pub

# 步骤一下载源码
in-toto-run --step-name scm-checkout  --signing-key signer.pem -- mvn clean package

# 步骤二编译
in-toto-run --step-name build --materials "*.java" --products "*.class" --signing-key signer.pem -- mvn clean package


# 验证上面步骤
in-toto-verify 

```
