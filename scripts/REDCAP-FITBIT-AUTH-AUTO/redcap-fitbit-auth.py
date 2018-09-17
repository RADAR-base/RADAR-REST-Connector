from redcap import Project, RedcapError
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

import requests
import json
import yaml
import codecs
import time

with open("config.yml", "r") as fi:
    CONFIG = yaml.load(fi)

class UserData(object):
    def __init__(self, fitbitpwd, fitbituser, subjectId, sourceId, projectId, enrolmentDate):
        self.fitbitpwd = fitbitpwd
        self.fitbituser = fitbituser
        self.subjectId = subjectId
        self.sourceId = sourceId
        self.projectId = projectId
        self.enrolmentDate = enrolmentDate


def parseAuthUrl(currentUrl):
    return currentUrl.split('=')[1].split('#')[0]

def isValidResponse(status_code):
    return status_code >= 200 and status_code < 300

def notExists(value):
    return value is None or value == ''

def getRedcapRecords(api_url, api_token):
    # Get data from Redcap
    try:
        project = Project(api_url, api_token)
    except RedcapError as er:
        print('Error getting Redcap Project {}', format(er))

    data = project.export_records()
    dataList = []
    for d in data:
        fitbitpwd = d['fitbit_password']
        fitbituser = d['fitbit_email']
        subjectId = d['subject_id']
        sourceId = d['fitbit_serial']
        enrolmentDate = d['enrolment_date']

        if notExists(fitbitpwd):
            fitbitpwd = CONFIG['FITBIT_BOT']['DEFAULT_FITBIT_PASSWORD']
        if notExists(enrolmentDate):
            enrolmentDate = CONFIG['DATES']['DATA_PULL_START_DATE_DEFAULT']
        else:
            # Date in redcap is in form yyyy-MM-dd, Make it consistent with other dates
            enrolmentDate = ''.join([enrolmentDate, 'T00:00:00Z'])
        if not notExists(fitbituser):
            dataList.append(UserData(fitbitpwd, fitbituser,
                                    subjectId, sourceId, CONFIG['REDCAP']['PROJECT_NAME'], enrolmentDate))
    return dataList

def getAuthCode(user):
    browser = webdriver.Chrome(CONFIG['CHROME_DRIVER_PATH'])
    # Automate Authorization and get Authorization code
    browser.get(CONFIG['FITBIT_AUTH']['FITBIT_AUTH_URL'])
    print user.fitbituser
    browser.execute_script("document.querySelectorAll('%s')[1].value = '%s'" % (
        CONFIG['FITBIT_BOT']['EMAIL_SELECTOR'], user.fitbituser))
    browser.execute_script("document.querySelectorAll('%s')[1].value = '%s'" % (
        CONFIG['FITBIT_BOT']['PASSWORD_SELECTOR'], user.fitbitpwd))
    browser.execute_script(
        "document.querySelectorAll('%s')[0].click()" % (CONFIG['FITBIT_BOT']['LOGIN_BUTTON_SELECTOR']))
    try:
        checkAll = WebDriverWait(browser, 3).until(
            EC.presence_of_element_located((By.ID, "selectAllScope"))
        )
        checkAll.click()
        allowButton = browser.find_element_by_id('allow-button')
        allowButton.click()
        return parseAuthUrl(browser.current_url)
    except Exception:
        # if already authorized before then directly generates the auth code
        return parseAuthUrl(browser.current_url)
    finally:
        browser.quit()


def getTokensUsingAuthCode(auth_code):
    # Use auth code to get access and refresh tokens
    postHeader = {
        'Authorization': 'Basic %s' % (CONFIG['FITBIT_AUTH']['FITBIT_BASIC_AUTH']),
        'Content-Type': 'application/x-www-form-urlencoded'
    }
    postData = {
        'clientId': CONFIG['FITBIT_AUTH']['FITBIT_CLIENT_ID'],
        'grant_type': 'authorization_code',
        'redirect_uri': CONFIG['FITBIT_AUTH']['FITBIT_REDIRECT_URI'],
        'code': auth_code
    }

    postResult = requests.post(
        "https://api.fitbit.com/oauth2/token", data=postData, headers=postHeader)

    print postResult.status_code
    print postResult.text
    if isValidResponse(postResult.status_code):
        jsonData = json.loads(postResult.text)
        return jsonData


def getOutputData(auth_data, user):
    oauthData = {
        'accessToken': authData['access_token'],
        'refreshToken': authData['refresh_token']
    }
    outputData = {
        'id': eachUser.fitbituser,
        'projectId': eachUser.projectId,
        'userId': eachUser.subjectId,
        'sourceId': 'charge2-%s' % (eachUser.sourceId),
        'startDate': eachUser.enrolmentDate,
        'endDate': CONFIG['DATES']['DATA_PULL_END_DATE_DEFAULT'],
        'fitbitUserId': authData['user_id'],
        'oauth2': oauthData
    }
    return outputData

def dumpToYamlFile(outputData, file_path):
    with codecs.open(file_path, "w", encoding="utf-8") as fo:
        yaml.safe_dump(outputData, fo,  default_flow_style=False)

def formatFileName(name, extenstion):
    finalName = name.replace(".", "_")
    finalName = finalName.replace("@", "_")
    return "".join(['fitbit-user-', finalName, extenstion])

outputDataDict = []
# to generalize the storeage replace this getRedcapRecords with any datasource supplying Fitbit credentials & login
dataList = getRedcapRecords(CONFIG['REDCAP']['REDCAP_API_URL'], CONFIG['REDCAP']['TOKEN'])
for eachUser in dataList:
    try:
        # Do fitbit auth
        authCode = getAuthCode(user=eachUser)
        authData = getTokensUsingAuthCode(auth_code=authCode)
        # Write all information to file in yaml format so fitbit connector can easily read them
        outputData = getOutputData(auth_data=authData, user=eachUser)
        outputDataDict.append(outputData)
        dumpToYamlFile(outputData=outputData, file_path=formatFileName(eachUser.fitbituser, ".yml"))
    except Exception as e:
        # If error occurs, log and continue with other users
        print('Error processing data {}', format(e))
        pass

dumpToYamlFile(outputData=outputData, file_path=formatFileName('all', ".yml"))
