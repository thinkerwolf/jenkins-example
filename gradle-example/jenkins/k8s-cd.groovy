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
    }
}
