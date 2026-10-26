
pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                bat '''
                    set "JAVA_HOME=C:\\Program Files\\Java\\jdk-21"
                    set "PATH=%JAVA_HOME%\\bin;%PATH%"
                    call mvnw.cmd -B clean package -DskipTests
                '''
            }
        }

        stage('Test') {
            steps {
                bat '''
                    set "JAVA_HOME=C:\\Program Files\\Java\\jdk-21"
                    set "PATH=%JAVA_HOME%\\bin;%PATH%"
                    call mvnw.cmd -B test
                '''
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'target/*.jar', allowEmptyArchive: true, fingerprint: true
        }
    }
}
