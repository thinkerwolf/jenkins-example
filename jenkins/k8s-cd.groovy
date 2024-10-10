pipeline {
    agent {
        label 'agent-sit'
    }

    environment {
        OSS_PATH = 'example/k8s-ci-example'
        CI_BUILD_NUMBER = 10
        kubeCredentials = 'eks-126026337867-uat-vod'
    }

    stages {
        stage('Deploy') {
            steps {
                ossDownload ossId: 'oss', path: '${OSS_PATH}/${CI_BUILD_NUMBER}/', location: 'deploy.yaml'
                sh 'ls -hl'
                withKubeCredentials(kubectlCredentials: [[credentialsId: kubeCredentials]]) {
                    sh 'kubectl apply -f deploy.yaml'
                }
            }
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
                                "👤 **执  行 者**: ${env.BUILD_USER}",
                                "<at id=all></at>"
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
}
