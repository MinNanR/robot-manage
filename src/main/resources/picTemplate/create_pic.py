from selenium.webdriver.chrome.options import Options
from selenium import webdriver
from selenium.webdriver.common.by import By
import sys

if __name__ == '__main__':
    driver_path = r"/usr/local/driver/chromedriver"
    argv = sys.argv
    html_path = argv[1]
    png_path = argv[2]
    chrome_options = Options()
    chrome_options.add_argument("--headless")
    chrome_options.add_argument("--disable-gpu")
    chrome_options.add_argument("--disable-dev-shm-usage")
    chrome_options.add_argument("--no-sandbox")
    driver = webdriver.Chrome(
        options=chrome_options, executable_path=driver_path)
    # driver.get("file:" + path)
    driver.set_window_size(1300, 800)
    driver.implicitly_wait(1)
    driver.get("File:" + html_path)
    driver.implicitly_wait(10)
    # driver.get_screenshot_as_file('test.png')
    driver.find_element(By.ID, "main").screenshot(png_path)
    driver.quit()