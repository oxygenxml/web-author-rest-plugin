# Debugging Guide

If you encounter issues with integrating your API, enabling debug logs can help you  to diagnose problems. 

## Enabling Debug Logs

Follow these steps to enable debug logging:

1. **Open the Administration Page**

2. **Navigate to Logging Settings**
    - Go to `Settings > General > Logging`.
    - This section contains the `Log file` path and the `Config file` path.

3. **Modify the Config File**
    - Locate and open the `Config file`.
    - Add the following line:
      ```
      log4j.category.com.oxygenxml.rest.plugin=debug
      ```
    - This line enables debug logs for the REST API.

4. **Save and Restart**
    - Save the changes made in the `Config file`.
    - Restart the server to apply these changes.
      
5. **View the Logs**
    - To view the generated logs, open the `Log file`.

## Disable Debug Logs

🚨 **Disable Debug Logs After Debugging**: To maintain optimal performance and prevent unnecessary log file growth, remember to remove the debug line from the `Config file` after you've finished debugging.
