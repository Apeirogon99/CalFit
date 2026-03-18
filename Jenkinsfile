pipeline {
    agent any

    environment {
        AWS_REGION   = 'ap-northeast-2'
        ECR_REGISTRY = credentials('ecr-registry')  // Jenkins Credential에 등록
        ECR_REPO     = 'calfit-be'
        EKS_CLUSTER  = 'calfit-cluster'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Gradle Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew :core:core-api:bootJar -x test'
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${ECR_REGISTRY}/${ECR_REPO}:${BUILD_NUMBER} -t ${ECR_REGISTRY}/${ECR_REPO}:latest ."
            }
        }

        stage('ECR Push') {
            steps {
                sh "aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
                sh "docker push ${ECR_REGISTRY}/${ECR_REPO}:${BUILD_NUMBER}"
                sh "docker push ${ECR_REGISTRY}/${ECR_REPO}:latest"
            }
        }

        stage('Deploy to EKS') {
            steps {
                sh "aws eks update-kubeconfig --region ${AWS_REGION} --name ${EKS_CLUSTER}"
                sh "kubectl set image deployment/coffiiness backend=${ECR_REGISTRY}/${ECR_REPO}:${BUILD_NUMBER} -n default"
                sh "kubectl rollout status deployment/coffiiness -n default --timeout=300s"
            }
        }
    }

    post {
        success {
            echo "Deploy #${BUILD_NUMBER} to EKS completed successfully!"
        }
        failure {
            echo "Deploy #${BUILD_NUMBER} to EKS failed!"
        }
        always {
            sh "docker rmi ${ECR_REGISTRY}/${ECR_REPO}:${BUILD_NUMBER} || true"
        }
    }
}