# RedCap Fitbit Authorisation Auto
This script performs automatic OAuth2 authorisation against the FITBIT API by using User credentials set up in a field in a RedCap project.
This is useful for getting Access and Refresh Tokens for hundreds of users automatically which would otherwise have to be done manually and hence a waste of time.
The output of this is in yaml formatted config files for each user which is directly consumable by the [Fitbit Source Connector](https://github.com/RADAR-base/RADAR-REST-Connector).

## Usage

1. First Copy the `config.yml.template` to `config.yml` and fill in the required configuration. Most of the config options are explained via comments the file.

2. This project needs the `ChromeDriver` to run. Please Download it from -- http://chromedriver.chromium.org/downloads and put the path of it in the `config.yml` file's `CHROME_DRIVER_PATH` parameter. This is required to open a chrome browser window for opening the fitbit OAuth page.

3. Install all the dependencies of required by the python script. Make sure `python 2` and `pip` are installed and then run -
    ```sh
      pip install -r requirements.txt
    ```

4. Run the program like -
    ``` sh
      python redcap-fitbit-auth.py
    ```
    This will then iterate through all the records in RedCap, performing OAuth authorisation on Fitbit API and creating a yml users config file (with fitbit tokens and user id) for each user in the same format as specified [here](https://github.com/RADAR-base/RADAR-REST-Connector/blob/master/docker/fitbit-user.yml.template).
    The projectId, userId, sourceId and startDate fields in the user config are directly taken from RedCap. The name of the generated files is the same as the fitbit user email (from redcap) with `.` and `@` replaced with `-` and prefixed with `fitbit-user-`. For eg, `radar.user.000001@gmail.com` will be `fitbit-user-radar_user_000001_gmail_com`
