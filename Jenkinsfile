// -*- mode: groovy; -*-

node {
  currentBuild.result = 'SUCCESS'

  def buildPrefix = 'dev'
  sh 'git rev-parse HEAD > commit'
  def commitHash = readFile('commit').trim()
  def permaBuildPrefix = 'builds/dev/${commitHash}'
  switch (env.BRANCH_NAME) {
    case 'release':
      buildPrefix = ''
      permaBuildPrefix = 'builds/release/${commitHash}';
      break;

    case 'staging':
      buildPrefix = 'staging'
      permaBuildPrefix = 'builds/staging/${commitHash}';
      break;
  }

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
      sh "ZETAWAR_BUILD_PREFIX=${buildPrefix} boot --no-colors build-site -e ${ZETAWAR_ENV}"
      sh "ZETAWAR_BUILD_PREFIX=${permaBuildPrefix} boot --no-colors build-site -e ${ZETAWAR_ENV} -t target.permabuild"
    }

    stage('Deploy') {
      sh "./bin/deploy -b ${S3_BUCKET} -p '${permaBuildPrefix}' ${PUBLIC_FLAG}"
      sh "./bin/deploy -s target.permabuild -b ${S3_BUCKET} -p ${permaBuildPrefix} ${PUBLIC_FLAG}"
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
