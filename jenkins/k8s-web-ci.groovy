pipeline {
    // Agent定义
    agent {
        // 流水线运行在基于Kubernetes POD的动态agent中
        kubernetes {
            // PodTemplate，配置位置在：系统管理->Clouds->kubernetes->Pod Templates
            inheritFrom 'nodejs-base'
        }
    }

    // 使用的工具定义，配置位置在：系统管理->全局工具配置
    tools {
        nodejs 'node-14'
    }

    // 变量定义
    environment {
        // Nexus镜像仓库URI
        REPOSITORY_URI = 'nexus-docker.bluebell015.xyz/docker'
        // Nexus镜像仓库使用的协议
        REPOSITORY_PROTOCOL = 'https'
        // Nexus镜像仓库访问凭证，在Credentials中配置
        NEXUS_CRED = credentials('nexus-registry')
        BASE_DIR = '.'
        IMAGE_NAME = 'ms-manage-web'
        SERVICE_NAME = 'ms-manage-web'
        NAMESPACE = 'membersite'
        DESIRED_COUNT = '1'
        // git凭证，在Credentials中配置 git2_dev.man
        gitCredential = 'git2_token'
        nginxConf = 'ms-manage-web-nginx'
    }
    // 运行Stages
    stages {

        stage('SCM checkout Build') {
            steps {
                nodejs('node-14') {
                    container('git') {
                        git branch: "${GIT_BRANCH}", credentialsId: gitCredential, url: 'http://git2.hgggggh.com:9981/SPC/system/membersite/ms-manage-web.git'
                        sh 'npm install --unsafe-perm=true --allow-root'
                        sh 'npm run build:release -- --dest=build'
                    }
                }
            }
        }

        stage('Upload') {
            steps {
                // 此step在Agent Pod的buildah容器运行
                configFileProvider([configFile(fileId: env.nginxConf, targetLocation: 'onlinestore.conf')]) {
                    container('buildah') {
                        script {
                            def imageRepo = "$REPOSITORY_URI" + "/$IMAGE_NAME"
                            def imageTag = new Date().format('yyyyMMddHHmmss') + "-${env.BUILD_ID}"
                            def imageName = imageRepo + ":" + imageTag
                            env.IMAGE_REPO = imageRepo
                            env.IMAGE_TAG = imageTag
                            env.IMAGE = imageName
                            sh '''
                        buildah login -u $NEXUS_CRED_USR -p $NEXUS_CRED_PSW $REPOSITORY_PROTOCOL://$REPOSITORY_URI
                        buildah build  --storage-driver vfs -t $IMAGE -f $BASE_DIR/Dockerfile $BASE_DIR
                        buildah push --storage-driver vfs $IMAGE
                        '''
                            def deployFilePath = "deployment.yaml"
                            // 替换k8s文件镜像
                            contentReplace(configs: [
                                    fileContentReplaceConfig(configs: [
                                            fileContentReplaceItemConfig(replace: env.IMAGE, search: '\\$IMAGE'),
                                    ], fileEncoding: 'UTF-8', filePath: deployFilePath, lineSeparator: 'Unix')
                            ])
                            // 最新的deploy文件上传oss
                            ossUpload ossId: 'oss', includes: deployFilePath, pathPrefix: '${JOB_NAME}/${BUILD_NUMBER}/'
                        }
                    }
                }
            }
        }
    }

    post {
        // 构建成功后触发cd作业
        success {
            build job: 'spc-uat/spc-cd-k8s-uat', wait: false, parameters: [
                    string(name: 'DEPLOYMENT_FILE', value: 'deployment.yaml'),
                    string(name: 'CI_JOB_NAME', value: env.JOB_NAME),
                    string(name: 'CI_BUILD_NUMBER', value: env.BUILD_NUMBER)
            ]
        }
        always {
            lark(
                    robot: "lark-spc",
                    type: "CARD",
                    title: "📢 Jenkins CI构建通知",
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
