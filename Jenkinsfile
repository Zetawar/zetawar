// -*- mode: groovy; -*-

node {
  currentBuild.result = 'SUCCESS'


  try {
    stage('Checkout') {
      checkout scm
    }

    stage('Prepare') {
      sh "npm install"
    }

    stage('Test') {
      sh "boot --no-colors run-tests"
    }

    stage('Build') {
      sh "boot --no-colors build-site -e ${ZETAWAR_ENV} ${PUBLIC_FLAG}"
    }

    stage('Deploy') {
      sh "./bin/deploy -b ${S3_BUCKET}"
    }
  } catch (err) {
    currentBuild.result = 'FAILURE'
    throw err
  } finally {
    notifyBuild(currentBuild.result)
  }
}

def notifyBuild(String buildStatus = 'STARTED') {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESS'

  // Default values
  def colorName = 'RED'
  def colorCode = '#FF0000'
  def recipients = UNSUCCESSFUL_RECIPIENTS
  def subject = "${env.JOB_NAME} - Build # ${env.BUILD_NUMBER} - ${buildStatus}!"
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """\
${env.JOB_NAME} - Build # ${env.BUILD_NUMBER} - ${buildStatus}

Check console output at ${env.BUILD_URL} to view the results.
"""

  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESS') {
    color = 'GREEN'
    colorCode = '#00FF00'
    recipients = SUCCESS_RECIPIENTS

    if (BACKER_BUILD == 'true') {
      subject = "A new Zetawar build is available!"
      summary = "${subject} (http://dev.zetawar.com/)"
      urlDetails = "A new Zetawar build is available at http://dev.zetawar.com/."
      loginDetails = "Login as user:${DEV_SITE_USER} with password:${DEV_SITE_PASSWORD}."
      changeDetails = sh(script: 'git log --pretty="- %s" --since="7 days ago"', returnStdout: true)
      footerDetails = """\
You're getting this email because you indicated you would like to receive build
notifications when you filled out the Zetawar Kickstarter survey. If you no
longer want to receive build notifications, please email builds@zetawar.com.
""".split("\n").join(" ")
      details = """\
${urlDetails} ${loginDetails}

Recent changes:
${changeDetails}

${footerDetails}
"""
    }
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }

  // Send notifications
  if (SEND_NOTIFICATIONS == 'true' && (buildStatus != 'SUCCESS' || BACKER_BUILD == 'true')) {
    emailext (
      to: recipients,
      replyTo: REPLY_TO,
      subject: subject,
      body: details,
    )
  }
}
