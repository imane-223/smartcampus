// TP Jenkins - verification du declenchement GitHub
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
                bat 'mvnw.cmd -B -DskipTests clean package'
            }
        }

        stage('Test') {
            steps {
                bat 'mvnw.cmd -B test'
            }
        }
    }
}
