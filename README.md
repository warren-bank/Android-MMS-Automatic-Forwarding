### [MMS Forwarding](https://github.com/warren-bank/Android-MMS-Automatic-Forwarding)

Android app that listens for incoming MMS multimedia messages and conditionally forwards them to other numbers.

#### Notes:

* minimum supported version of Android:
  - Android 5.0 (API level 21)

#### Screenshots:

![MMS-Forwarding](./screenshots/1-prefs-no-entries-in-whitelist.png)
![MMS-Forwarding](./screenshots/2-prefs-add-new-entry-dialog.png)
![MMS-Forwarding](./screenshots/3-prefs-one-entry-in-whitelist.png)

- - - -

### Configuration

#### Forwarding Rules:

* `Enable Service` checkbox:
  - used to enable/disable this service
* `ADD` ActionBar menu item:
  - adds new forwarding rule
* forwarding rule entries are defined as follows:
  - `Forwarding recipient`:
    * a valid phone number (without any punctuation)
  - `Sender must end with`:
    * this value specifies a phone number (without any punctuation)
      - a match occurs when the _sender_ of an incoming MMS message ends with this exact value
      - a special match-all glob pattern `*` is supported,<br>which can either be manually entered into the field<br>or automatically set when the field is left empty
    * this value acts as a filter
      - `Forwarding recipient` will only receive copies of incoming MMS messages that match this value
* forwarding rule entries can be modified
  - clicking on an existing recipient opens a dialog with options to:
    * edit field values, and save changes
    * delete

- - - -

#### Build Flavors:

* `english`
  - includes string resources for only the English language
* `withAllLanguageTranslations`
  - includes string resources for additional languages
* `noContactName`
  - "Forwarded from: &lt;sender_phone&gt;"
* `withContactName`
  - "Forwarded from: &lt;sender_name&gt; &lt;sender_phone&gt;"
    * if `sender_phone` is found in the local contacts list,<br>and the contact has a name associated with it
  - "Forwarded from: &lt;sender_phone&gt;"
    * otherwise

- - - -

#### Caveats:

* _Google Voice_:
  - when an MMS is sent from a _Google Voice_ number to the SIM card number&hellip;
    * everything works as expected, but there are [limitations](https://support.google.com/voice/thread/162979587/send-mms-messages?hl=en):
      ```text
      Google Voice only supports sending the following file formats via MMS:
        .jpg .png and .gif

      There are file size limits for each format.
      ```
  - when an MMS is sent to a _Google Voice_ number&hellip;
    * the sender of the MMS message will always be a number belonging to the _Google Voice_ backend infrastructure, which is important to remember when setting forwarding filters

#### Legal:

* copyright: [Warren Bank](https://github.com/warren-bank)
* license: [GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)
