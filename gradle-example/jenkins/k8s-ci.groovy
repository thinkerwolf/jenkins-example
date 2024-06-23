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
        BASE_DIR = 'gradle-example'
        IMAGE_NAME = 'gradle-example'
        gitCredential = 'github-thinkerwolf'
        //kubeCredentials = 'eks-126026337867-uat-vod'
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
                        sh 'gradle build -x test --stacktrace --scan --refresh-dependencies -b $BASE_DIR/build.gradle'
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
                        ossUpload ossId: 'oss', includes: deployFilePath, pathPrefix: '${JOB_NAME}/${BUILD_NUMBER}/'
                    }
                }
            }
        }

        /*stage('Deploy') {
            agent {
                label 'agent-sit'
            }
            steps {
                ossDownload ossId: 'op-oss', path: '${JOB_NAME}/${BUILD_NUMBER}/', location: 'deploy.yaml'
                sh 'ls -hl'
                withKubeCredentials(kubectlCredentials: [[credentialsId: kubeCredentials]]) {
                    sh 'kubectl apply -f deploy.yaml'
                }
            }
        }*/

    }

}
