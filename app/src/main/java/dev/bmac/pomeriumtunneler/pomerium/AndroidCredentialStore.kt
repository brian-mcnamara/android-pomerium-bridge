package dev.bmac.pomeriumtunneler.pomerium

import android.content.Context
import androidx.core.content.edit
import com.salesforce.pomerium.CredentialKey
import com.salesforce.pomerium.CredentialStore

class AndroidCredentialStore(private val applicationContext: Context): CredentialStore {
    val sharedPreferences = applicationContext.getSharedPreferences("instanceKeys", 0)

    override fun getToken(key: CredentialKey): String? {
        return sharedPreferences.getString(key, null)
    }

    override fun setToken(key: CredentialKey, jwt: String) {
        sharedPreferences.edit {
            putString(key, jwt)
        }
    }

    override fun clearToken(key: CredentialKey) {
        sharedPreferences.edit {
            remove(key)
        }
    }
}