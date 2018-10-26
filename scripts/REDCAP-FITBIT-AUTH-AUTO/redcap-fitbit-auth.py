#!/usr/bin/env python2

from redcap import Project, RedcapError
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

import requests
import json
import yaml
import codecs


class UserData:
    def __init__(self, redcap_entry=None, default_password=None,
                 default_start_date='2017-01-01T00:00:00Z', project_name=None):
        self.project_id = project_name

        if redcap_entry is not None:
            self.fitbit_password = redcap_entry['fitbit_password']
            self.fitbit_username = redcap_entry['fitbit_email']
            self.user_id = redcap_entry['subject_id']
            self.source_id = redcap_entry['fitbit_serial']
            self.enrolment_date = redcap_entry['enrolment_date']

        if not self.fitbit_password:
            self.fitbit_password = default_password

        if self.enrolment_date:
            # Date in redcap is in form yyyy-MM-dd
            # Make it consistent with other dates
            self.enrolment_date = '%sT00:00:00Z' % self.enrolment_date
        else:
            self.enrolment_date = default_start_date


def parse_auth_url(current_url):
    return current_url.split('=')[1].split('#')[0]


def is_valid_response(status_code):
    return 200 <= status_code < 300


def not_exists(value):
    return value is None or value == ''


def get_redcap_records(api_url, api_token, default_password,
                       default_start_date, project_name):
    # Get data from Redcap
    try:
        project = Project(api_url, api_token)
        user_records = project.export_records()
    except RedcapError as er:
        raise IOError('Error getting Redcap Project {}', er)

    return [UserData(d, default_password, default_start_date, project_name)
            for d in user_records]


class ChromeFitbitScreen:
    def __init__(self, driver_path, fitbit_auth_url, email_selector,
                 password_selector, login_button_selector):
        self.password_selector = password_selector
        self.login_button_selector = login_button_selector
        self.email_selector = email_selector
        self.browser = webdriver.Chrome(driver_path)
        self.fitbit_auth_url = fitbit_auth_url

    def get_auth_code(self, user):
        self.browser.get(self.fitbit_auth_url)
        print(user.fitbit_username)
        self.browser.execute_script(
            "document.querySelectorAll('%s')[1].value = '%s'"
            % (self.email_selector, user.fitbit_username))
        self.browser.execute_script(
            "document.querySelectorAll('%s')[1].value = '%s'"
            % (self.password_selector, user.fitbit_password))
        self.browser.execute_script(
            "document.querySelectorAll('%s')[0].click()"
            % self.login_button_selector)
        try:
            check_all = WebDriverWait(self.browser, 3).until(
                EC.presence_of_element_located((By.ID, "selectAllScope"))
            )
            check_all.click()
            allow_button = self.browser.find_element_by_id('allow-button')
            allow_button.click()
            return parse_auth_url(self.browser.current_url)
        except Exception:
            # if already authorized before then directly generates the auth code
            return parse_auth_url(self.browser.current_url)

    def quit(self):
        self.browser.quit()


def get_tokens_using_auth_code(auth_code, client_id, client_secret, redirect_uri):
    # Use auth code to get access and refresh tokens
    post_header = {
        'Content-Type': 'application/x-www-form-urlencoded'
    }
    post_data = {
        'clientId': client_id,
        'grant_type': 'authorization_code',
        'redirect_uri': redirect_uri,
        'code': auth_code
    }

    post_result = requests.post(
        "https://api.fitbit.com/oauth2/token",
        data=post_data, headers=post_header,
        auth=(client_id, client_secret))

    if not is_valid_response(post_result.status_code):
        raise IOError("Cannot load token from Fitbit API")

    return json.loads(post_result.text)


def get_output_data(auth_data, user, default_end_date):
    return {
        'id': user.fitbit_username,
        'projectId': user.project_id,
        'userId': user.user_id,
        'sourceId': 'charge2-%s' % user.source_id,
        'startDate': user.enrolment_date,
        'endDate': default_end_date,
        'externalUserId': auth_data['user_id'],
        'oauth2': {
          'accessToken': auth_data['access_token'],
          'refreshToken': auth_data['refresh_token']
        }
    }


def dump_to_yaml(output_data, file_path):
    with codecs.open(file_path, "w", encoding="utf-8") as fo:
        yaml.safe_dump(output_data, fo, default_flow_style=False)


def format_file_name(name, extension):
    final_name = name.replace(".", "_")
    final_name = final_name.replace("@", "_")
    return "".join(['fitbit-user-', final_name, extension])


def register_users():
    with open("config.yml", "r") as fi:
        config = yaml.load(fi)

    output_data_dict = []
    # to generalize the storeage replace this getRedcapRecords with any
    # datasource supplying Fitbit credentials & login
    users = get_redcap_records(
            config['REDCAP']['REDCAP_API_URL'],
            config['REDCAP']['TOKEN'],
            config['FITBIT_BOT']['DEFAULT_FITBIT_PASSWORD'],
            config['DATES']['DATA_PULL_START_DATE_DEFAULT'],
            config['REDCAP']['PROJECT_NAME'])
    default_end_date = config['DATES']['DATA_PULL_END_DATE_DEFAULT']

    chrome = ChromeFitbitScreen(
            config['CHROME_DRIVER_PATH'],
            config['FITBIT_AUTH']['FITBIT_AUTH_URL'],
            config['FITBIT_BOT']['EMAIL_SELECTOR'],
            config['FITBIT_BOT']['PASSWORD_SELECTOR'],
            config['FITBIT_BOT']['LOGIN_BUTTON_SELECTOR'])

    for user in users:
        try:
            # Do fitbit auth
            auth_code = chrome.get_auth_code(user=user)
            auth_data = get_tokens_using_auth_code(
                    auth_code=auth_code,
                    client_id=config['FITBIT_AUTH']['FITBIT_CLIENT_ID'],
                    client_secret=config['FITBIT_AUTH']['FITBIT_CLIENT_SECRET'],
                    redirect_uri=config['FITBIT_AUTH']['FITBIT_REDIRECT_URI'])
            # Write all information to file in yaml format so fitbit connector
            # can easily read them
            output_data = get_output_data(auth_data=auth_data, user=user,
                                          default_end_date=default_end_date)
            output_data_dict.append(output_data)
            dump_to_yaml(
                output_data=output_data,
                file_path=format_file_name(user.fitbit_username, ".yml"))
        except Exception as e:
            # If error occurs, log and continue with other users
            print('Error processing data for user {}: {}'.format(user, e))
            pass

    chrome.quit()

    dump_to_yaml(output_data=output_data_dict,
                 file_path=format_file_name('all', ".yml"))


if __name__ == '__main__':
    register_users()
