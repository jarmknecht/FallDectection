package com.example.jonathan.fall_detection;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

class ContactsController {

    private Context context;

    ContactsController(Context context) { this.context = context; }

    JSONArray getCurrentContacts(JSONArray contacts) {
        JSONObject prevMax = null;
        JSONObject currMax = null;
        for (int i = 0; i < contacts.length(); ++i) {
            try {
                JSONObject contact = contacts.getJSONObject(i);
                int timesContacted = contact.getInt("times_contacted");
                if(contact.getInt("times_contacted") > (currMax != null ? currMax.getInt("times_contacted") : 0)) {
                    prevMax = currMax;
                    currMax = contact;
                } else if (timesContacted > (prevMax != null ? prevMax.getInt("times_contacted") : 0)) {
                    prevMax = contact;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        JSONArray maxContactedContacts = new JSONArray();
        if (currMax != null)
            maxContactedContacts.put(currMax);
        if (prevMax != null)
            maxContactedContacts.put(prevMax);
        return maxContactedContacts;
    }

    JSONArray getCurrentContacts() {
        JSONArray contacts = getContacts();
        JSONObject prevMax = null;
        JSONObject currMax = null;
        for (int i = 0; i < contacts.length(); ++i) {
            try {
                JSONObject contact = contacts.getJSONObject(i);
                int timesContacted = contact.getInt("times_contacted");
                if(contact.getInt("times_contacted") > (currMax != null ? currMax.getInt("times_contacted") : 0)) {
                    prevMax = currMax;
                    currMax = contact;
                } else if (timesContacted > (prevMax != null ? prevMax.getInt("times_contacted") : 0)) {
                    prevMax = contact;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        JSONArray maxContactedContacts = new JSONArray();
        if (currMax != null)
            maxContactedContacts.put(currMax);
        if (prevMax != null)
            maxContactedContacts.put(prevMax);
        return maxContactedContacts;
    }

    HashMap<String, String> getContactsMap(){
        JSONArray contacts = getContacts();
        HashMap<String, String> contactsMap = new HashMap<>();
        for (int i = 0; i < contacts.length(); ++i) {
            try {
                JSONObject contact = contacts.getJSONObject(i);
                contactsMap.put(PhoneNumberUtils.stripSeparators(contact.getString("phone_number")), contact.getString("name"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return contactsMap;
    }

    JSONArray getContacts() {
        JSONArray contacts = new JSONArray();
        ContentResolver cr = context.getContentResolver();
        Cursor cur = cr.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null);
        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur.moveToNext()) {
                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID))},
                            null);
                    while (pCur != null && pCur.moveToNext()) {
                        try {
                            contacts.put(new JSONObject()
                                .put("name", cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)))
                                .put("phone_number", pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)))
                                .put("times_contacted", cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED))));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if (pCur != null)
                        pCur.close();
                }
            }
        }
        if(cur!=null)
            cur.close();
        return contacts;
    }
}
