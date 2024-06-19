pipeline {
    agent {
        kubernetes {
            inheritFrom 'gradle-jdk17'
        }
    }

    tools {
        gradle 'gradle-8.8'
    }

    environment {
        REPOSITORY_URI = 'nexus-docker.bluebell015.xyz/docker'
        REPOSITORY_PROTOCOL = 'https'
        NEXUS_CRED = credentials('nexus-registry')
        BASE_DIR = './gradle-example'
        IMAGE_NAME = 'gradle-example'
        gitCredential = 'github-thinkerwolf'
    }

    stages {
        stage('SCM checkout') {
            steps {
                container('git') {
                    git branch: 'main', credentialsId: gitCredential, url: 'https://github.com/thinkerwolf/jenkins-example.git'
                }
            }
        }

        stage('Build') {
            steps {
                withGradle {
                    container('jdk') {
                        sh 'gradle -v'
                        sh 'gradle build --no-daemon -x test --refresh-dependencies -b $BASE_DIR/build.gradle'
                        sh 'ls -hl $BASE_DIR/build'
                    }
                }

                container('buildah') {
                    script {
                        def imageName = "$REPOSITORY_URI" + "/$IMAGE_NAME:" + new Date().format('yyyyMMddHHmmss') + "-${env.BUILD_ID}"
                        env.IMAGE = imageName
                        sh '''
                        buildah login -u $NEXUS_CRED_USR -p $NEXUS_CRED_PSW $REPOSITORY_PROTOCOL://$REPOSITORY_URI
                        buildah build  --storage-driver vfs -t $IMAGE -f $BASE_DIR/Dockerfile $BASE_DIR
                        buildah push --storage-driver vfs $IMAGE
                        '''
                        def deployFilePath = "$BASE_DIR/deploy.yaml"
                        // 替换k8s文件镜像
                        contentReplace(configs: [
                                fileContentReplaceConfig(configs: [
                                        fileContentReplaceItemConfig(replace: imageName, search: '\\$IMAGE')
                                ], fileEncoding: 'UTF-8', filePath: deployFilePath, lineSeparator: 'Unix')
                        ])
                        // 最新的deploy文件上传oss
                        ossUpload ossId: 'op-oss', includes: '${BASE_DIR}/deploy.yaml', pathPrefix: '${JOB_NAME}/${BUILD_NUMBER}/'
                    }
                }
            }
        }
    }

}
