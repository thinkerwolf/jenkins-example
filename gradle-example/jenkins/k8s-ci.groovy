pipeline {
    // Agent定义
    agent {
        // 流水线运行在基于Kubernetes POD的动态agent中
        kubernetes {
            // PodTemplate，配置位置在：系统管理->Clouds->kubernetes->Pod Templates
            inheritFrom 'gradle-jdk17'
        }
    }

    // 使用的工具定义，配置位置在：系统管理->全局工具配置
    tools {
        // 使用全局工具配置中名为gradle-8.8的gradle
        gradle 'gradle-8.8'
    }

    // 变量定义
    environment {
        // Nexus镜像仓库URI
        REPOSITORY_URI = 'nexus-docker.bluebell015.xyz/docker'
        // Nexus镜像仓库使用的协议
        REPOSITORY_PROTOCOL = 'https'
        // Nexus镜像仓库访问凭证，在Credentials中配置
        NEXUS_CRED = credentials('nexus-registry')
        BASE_DIR = 'gradle-example'
        IMAGE_NAME = 'gradle-example'
        // git凭证，在Credentials中配置
        gitCredential = 'github-thinkerwolf'
        //kubeCredentials = 'eks-126026337867-uat-vod'
    }
    // 运行Stages
    stages {
        stage('SCM checkout') {
            steps {
                // 此step在Agent Pod的git容器运行
                container('git') {
                    git branch: 'main', credentialsId: gitCredential, url: 'https://github.com/thinkerwolf/jenkins-example.git'
                }
            }
        }

        stage('Build') {
            steps {
                // 使用全局工具中配置的gradle版本运行
                withGradle {
                    // 此step在Agent Pod的jdk容器运行
                    container('jdk') {
                        sh 'gradle -v'
                        sh 'gradle build -x test --stacktrace --scan --refresh-dependencies -b $BASE_DIR/build.gradle'
                        sh 'ls -hl $BASE_DIR/build'
                    }
                }
                // 此step在Agent Pod的buildah容器运行
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

    post {
        always {
            lark(
                    robot: "cicd-notify-test",
                    type: "CARD",
                    title: "📢 Jenkins 构建通知",
                    text: [
                            "📋 **任务名称**：[${JOB_NAME}](${JOB_URL})",
                            "🔢 **任务编号**：[${BUILD_DISPLAY_NAME}](${BUILD_URL})",
                            "🌟 **构建状态**: ${currentBuild.currentResult}",
                            "🕐 **构建用时**: ${currentBuild.duration} ms",
                            "👤 **执  行 者**: ${env.BUILD_USER}"
                    ],
                    buttons: [
                            [
                                    title: "更改记录",
                                    url  : "${BUILD_URL}changes"
                            ],
                            [
                                    title: "控制台",
                                    type : "danger",
                                    url  : "${BUILD_URL}console"
                            ]
                    ]
            )
        }
    }
}
