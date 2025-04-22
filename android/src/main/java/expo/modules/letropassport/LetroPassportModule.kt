package expo.modules.letropassport

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.Arguments
import expo.modules.kotlin.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import net.sf.scuba.smartcards.CardFileInputStream
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.*
import org.jmrtd.cert.CardVerifiableCertificate
import org.jmrtd.lds.AbstractTaggedLDSFile
import org.jmrtd.lds.ActiveAuthenticationInfo
import org.jmrtd.lds.CVCAFile
import org.jmrtd.lds.CardAccessFile
import org.jmrtd.lds.ChipAuthenticationInfo
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo
import org.jmrtd.lds.LDSFileUtil
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.SecurityInfo
import org.jmrtd.lds.icao.COMFile
import org.jmrtd.lds.icao.DG11File
import org.jmrtd.lds.icao.DG12File
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG15File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.icao.DG3File
import org.jmrtd.lds.icao.DG5File
import org.jmrtd.lds.icao.DG7File
import org.jmrtd.lds.icao.MRZInfo
import org.jmrtd.protocol.BACResult
import org.jmrtd.protocol.EACCAResult
import org.jmrtd.protocol.EACTAResult
import org.jmrtd.protocol.PACEResult
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.security.Security
import java.util.*
import org.jmrtd.lds.iso19794.FaceImageInfo
import org.json.JSONObject
import expo.modules.letropassport.ImageUtil
import android.provider.MediaStore


import android.content.Context
import android.content.SharedPreferences
import androidx.core.os.bundleOf

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.asn1.cms.SignedData
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1Set
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.icao.DataGroupHash;
import org.bouncycastle.asn1.icao.LDSSecurityObject;
import org.bouncycastle.asn1.x509.Certificate
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.bouncycastle.jce.interfaces.ECPublicKey


import com.google.gson.Gson;


import java.io.FileOutputStream
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertPathValidator
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.cert.X509Certificate
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import java.text.ParseException
import java.security.interfaces.RSAPublicKey
import java.text.SimpleDateFormat
import java.util.*
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

import androidx.appcompat.app.AppCompatActivity
import expo.modules.letropassport.ImageUtil.decodeImage
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Callback
import net.sf.scuba.smartcards.CardService
import org.apache.commons.io.IOUtils

import expo.modules.letropassport.passportreader.ui.activities.CameraActivity
import expo.modules.letropassport.passportreader.common.IntentData


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.google.gson.GsonBuilder

import expo.modules.letropassport.R
import java.io.FileInputStream

object Messages {
    const val SCANNING = "Scanning....."
    const val STOP_MOVING = "Stop moving....." 
    const val AUTH = "Auth....."
    const val COMPARING = "Comparing....."
    const val COMPLETED = "Scanning completed"
    const val RESET = "Reseting..."
}

class Response(json: String) : JSONObject(json) {
    val type: String? = this.optString("type")
    val data = this.optJSONArray("data")
        ?.let { 0.until(it.length()).map { i -> it.optJSONObject(i) } } 
        ?.map { Foo(it.toString()) } 
}

class Foo(json: String) : JSONObject(json) {
    val id = this.optInt("id")
    val title: String? = this.optString("title")
}

class LetroPassportModule : Module() {
  private val TAG = "ProverModule"
  private var scanPromise: Promise? = null
  private var opts: ReadableMap? = null
  private var dg1File: DG1File? = null
  private var dg2File: DG2File? = null
  private var promiseCamera: Promise? = null

  init {
    Security.insertProviderAt(BouncyCastleProvider(), 1)
  }

  override fun definition() = ModuleDefinition {
    Name("LetroPassport")
    
    Events("onChange")


    Function("hello") {
      "Hello From Letro Passport Module! 👋"
    }

    Function("isSupported") {
      appContext.reactContext?.packageManager?.hasSystemFeature(PackageManager.FEATURE_NFC)
    }

    AsyncFunction("cancel") { promise: Promise ->
      scanPromise?.reject(E_SCAN_CANCELED, "canceled",null)
      resetState()
      promise.resolve(null)
    }

    AsyncFunction("runProveAction") { zkey_path: String, witness_calculator: String, dat_file_path: String, inputs: ReadableMap, promise: Promise ->
          Log.e(TAG, "zkey_path in provePassport kotlin: $zkey_path")
    Log.e(TAG, "witness_calculator in provePassport kotlin: $witness_calculator")
    Log.e(TAG, "dat_file_path in provePassport kotlin: $dat_file_path")
    Log.e(TAG, "inputs in provePassport kotlin: ${inputs.toString()}")

    // Read the dat file from the provided filesystem path
    val datFile = File(dat_file_path)
    if (!datFile.exists()) {
        Log.e(TAG, "Dat file does not exist at path: $dat_file_path")
        throw IllegalArgumentException("Dat file does not exist at path: $dat_file_path")
    }

    val datBytes: ByteArray
    try {
        datBytes = datFile.readBytes()
    } catch (e: IOException) {
        Log.e(TAG, "Error reading dat file: ${e.message}")
        throw IllegalArgumentException("Error reading dat file: ${e.message}")
    }

    val formattedInputs = mutableMapOf<String, Any?>()

    val iterator = inputs.keySetIterator()
    while (iterator.hasNextKey()) {
        val key = iterator.nextKey()
        val array = inputs.getArray(key)?.toArrayList()?.map { it.toString() }
        formattedInputs[key] = if (array?.size == 1) array.firstOrNull() else array
    }

    val gson = GsonBuilder().setPrettyPrinting().create()
    Log.e(TAG, gson.toJson(formattedInputs))

    // (used to be) working example
    // val inputs = mutableMapOf<String, List<String>>(
    //   "mrz" to listOf("97","91","95","31","88","80","60","70","82","65","84","65","86","69","82","78","73","69","82","60","60","70","76","79","82","69","78","84","60","72","85","71","85","69","83","60","74","69","65","78","60","60","60","60","60","60","60","60","60","49","57","72","65","51","52","56","50","56","52","70","82","65","48","48","48","55","49","57","49","77","50","57","49","50","48","57","53","60","60","60","60","60","60","60","60","60","60","60","60","60","60","48","50"),
    //   "reveal_bitmap" to listOf("0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0"),
    //   "dataHashes" to listOf("48","130","1","37","2","1","0","48","11","6","9","96","134","72","1","101","3","4","2","1","48","130","1","17","48","37","2","1","1","4","32","99","19","179","205","55","104","45","214","133","101","233","177","130","1","37","89","125","229","139","34","132","146","28","116","248","186","63","195","96","151","26","215","48","37","2","1","2","4","32","63","234","106","78","31","16","114","137","237","17","92","71","134","47","62","78","189","233","201","213","53","4","47","189","201","133","6","121","34","131","64","142","48","37","2","1","3","4","32","136","155","87","144","121","15","152","127","85","25","154","80","20","58","51","75","193","116","234","0","60","30","29","30","183","141","72","247","255","203","100","124","48","37","2","1","11","4","32","0","194","104","108","237","246","97","230","116","198","69","110","26","87","17","89","110","199","108","250","36","21","39","87","110","102","250","213","174","131","171","174","48","37","2","1","12","4","32","190","82","180","235","222","33","79","50","152","136","142","35","116","224","6","242","156","141","128","247","10","61","98","86","248","45","207","210","90","232","175","38","48","37","2","1","13","4","32","91","222","210","193","63","222","104","82","36","41","138","253","70","15","148","208","156","45","105","171","241","195","185","43","217","162","146","201","222","89","238","38","48","37","2","1","14","4","32","76","123","216","13","52","227","72","245","59","193","238","166","103","49","24","164","171","188","194","197","156","187","249","28","198","95","69","15","182","56","54","38","128","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","9","72"),
    //   "datahashes_padded_length" to listOf("320"),
    //   "eContentBytes" to listOf("49","102","48","21","6","9","42","134","72","134","247","13","1","9","3","49","8","6","6","103","129","8","1","1","1","48","28","6","9","42","134","72","134","247","13","1","9","5","49","15","23","13","49","57","49","50","49","54","49","55","50","50","51","56","90","48","47","6","9","42","134","72","134","247","13","1","9","4","49","34","4","32","176","96","59","213","131","82","89","248","105","125","37","177","158","162","137","43","13","39","115","6","59","229","81","110","49","75","255","184","155","73","116","86"),
    //   "signature" to listOf("1004979219314799894","6361443755252600907","6439012883494616023","9400879716815088139","17551897985575934811","11779273958797828281","2536315921873401485","3748173260178203981","12475215309213288577","6281117468118442715","1336292932993922350","14238156234566069988","11985045093510507012","3585865343992378960","16170829868787473084","17039645001628184779","486540501180074772","5061439412388381188","12478821212163933993","7430448406248319432","746345521572597865","5002454658692185142","3715069341922830389","11010599232161942094","1577500614971981868","13656226284809645063","3918261659477120323","5578832687955645075","3416933977282345392","15829829506526117610","17465616637242519010","6519177967447716150"),
    //   "signatureAlgorithm" to listOf("1"),
    //   "pubkey" to listOf("9539992759301679521","1652651398804391575","7756096264856639170","15028348881266521487","13451582891670014060","11697656644529425980","14590137142310897374","1172377360308996086","6389592621616098288","6767780215543232436","11347756978427069433","2593119277386338350","18385617576997885505","14960211320702750252","8706817324429498800","15168543370367053559","8708916123725550363","18006178692029805686","6398208271038376723","15000821494077560096","17674982305626887153","2867958270953137726","9287774520059158342","9813100051910281130","13494313215150203208","7792741716144106392","6553490305289731807","32268224696386820","15737886769048580611","669518601007982974","11424760966478363403","16073833083611347461"),
    //   "pathIndices" to listOf("0","1","1","1","1","1","1","0","1","1","0","0","1","1","0","0"),
    //   "siblings" to listOf("20516282398390866580647417962347415258712802604212003365416596890852644939364","20547289806543281108128197867250295423223489766069952889766689677695750842294","17092860852967512812593771487649838995106203215624858397482169733546970246117","19141872343555753276227561835732941623954902346285308564941039231845690663515","2888260764701592030713638283446165050628606750519377550369633789586724212406","17037943129534065359096662792322618985598809624384219749636863003643326502177","21260541151470016589788332273091943678373855676584683193443363340566713593750","9681119423869145671286918102040570804786474221694907866875171055859965502010","3999714159260652982057321310481110903729446356195536109316994934664982988519","14359042263488593594514913785064471775842285148703143594475594381078274944550","10696856845043652409316424831381338144209147199074363427177722046972515079299","2796323689030312622891330190155708704921773618732461037692992858528069077360","1379184643939692456020535864077563679018059205165852146212742699309755722087","17834317267514482863629341626611816587254867008433493508231639322166589549456","1473918712602583605383280948484316645101117513102582419100942131704211814519","15819538789928229930262697811477882737253464456578333862691129291651619515538"),
    //   "root" to listOf("4080578225172475068086778061870548445929343471785864518431540330127324371840"),
    //   "address" to listOf("642829559307850963015472508762062935916233390536")
    // )


    val jsonInputs = gson.toJson(formattedInputs).toByteArray()
    val zkpTools = ZKPTools(appContext.reactContext!!)

    val witnessCalcFunction = when (witness_calculator) {
        "prove_rsa_65537_sha256" -> zkpTools::witnesscalc_prove_rsa_65537_sha256
        "prove_rsa_65537_sha1" -> zkpTools::witnesscalc_prove_rsa_65537_sha1
        "prove_rsapss_65537_sha256" -> zkpTools::witnesscalc_prove_rsapss_65537_sha256
        "vc_and_disclose" -> zkpTools::witnesscalc_vc_and_disclose
        else -> throw IllegalArgumentException("Invalid witness calculator name")
    }  
    
    val zkp: ZkProof = ZKPUseCase(appContext.reactContext!!).generateZKP(
        zkey_path,
        datBytes,
        jsonInputs,
        witnessCalcFunction
    )

    Log.e("ZKP", gson.toJson(zkp))

    promise.resolve(zkp.toString())
    }


    AsyncFunction("scan") { inComingopts: ReadableMap, promise: Promise ->
      val mNfcAdapter = NfcAdapter.getDefaultAdapter(appContext.reactContext)
      if (mNfcAdapter == null) {
        promise.reject(E_NOT_SUPPORTED, "NFC chip reading not supported",null)
        eventMessageEmitter("NFC chip reading not supported")
        return@AsyncFunction
      }

      if (!mNfcAdapter.isEnabled) {
        promise.reject(E_NOT_ENABLED, "NFC chip reading not enabled",null)
        eventMessageEmitter("NFC chip reading not enabled")
        return@AsyncFunction
      }

      if (scanPromise != null) {
        eventMessageEmitter("Already running a scan")
        promise.reject(E_ONE_REQ_AT_A_TIME, "Already running a scan",null)
        return@AsyncFunction
      }
      eventMessageEmitter(Messages.SCANNING)
      opts = inComingopts
      scanPromise = promise
    }
    
    AsyncFunction("startCameraActivity") { promise: Promise ->
      try {
        promiseCamera = promise
        val intent = Intent(appContext.reactContext, CameraActivity::class.java)
        appContext.currentActivity?.startActivityForResult(intent, 1)
      } catch (e: Exception) {
        promise.reject("ERROR", e.message, e)
      }
    }
    

    OnNewIntent { intent ->
        eventMessageEmitter("New intent received")
        Log.d("LetroPassport", "New intent received")
        handleNewIntent(intent)
    }
    OnActivityEntersForeground {
      eventMessageEmitter("Host resume")
      Log.d("LetroPassport", "Host resume")
      handleHostResume()
    }
    OnActivityEntersBackground {
      eventMessageEmitter("Host pause")
      Log.d("LetroPassport", "Host pause")
      handleHostPause()
    }
    OnDestroy {
      eventMessageEmitter("Host destroy")
      Log.d("LetroPassport", "Host destroy")
      handleHostDestroy()
    }
    
    OnActivityResult { activity, payload ->
            if (payload.requestCode == 1) {
                if (payload.resultCode == Activity.RESULT_OK) {
                    val mrzInfo = payload.data?.getSerializableExtra(IntentData.getKeyMrzInfo()) as? MRZInfo
                    if (mrzInfo != null) {
                        promiseCamera?.resolve(mrzInfo.toString()) // Or format as needed
                    } else {
                        promiseCamera?.reject("ERROR", "MRZ info not found",null)
                    }
                } else if (payload.resultCode == Activity.RESULT_CANCELED) {
                    promiseCamera?.reject("CANCELLED", "Camera activity cancelled",null)
                }
            }
    }

  }

  private fun handleHostResume() {
    val mNfcAdapter = NfcAdapter.getDefaultAdapter(appContext.reactContext)
    if (mNfcAdapter == null) {
      eventMessageEmitter("NFC chip reading not supported!")
      return
    }

    val activity = appContext.currentActivity
    val intent = Intent(activity?.applicationContext, activity?.javaClass)
    intent?.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    val pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE)
    val filter = arrayOf(arrayOf(IsoDep::class.java.name))
    mNfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, filter)
  }

  private fun handleHostPause() {
    val mNfcAdapter = NfcAdapter.getDefaultAdapter(appContext.reactContext)
    if (mNfcAdapter == null) return

    mNfcAdapter.disableForegroundDispatch(appContext.currentActivity)
  }

  private fun handleHostDestroy() {
    resetState()
  }

  private fun handleNewIntent(intent: Intent) {
    if (scanPromise == null) return
    if (NfcAdapter.ACTION_TECH_DISCOVERED != intent.action) return

    val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
    if (tag?.techList?.contains(IsoDep::class.java.name) != true) return

    val bacKey = BACKey(
      opts?.getString(PARAM_DOC_NUM),
      opts?.getString(PARAM_DOB),
      opts?.getString(PARAM_DOE)
    )

    ReadTask(IsoDep.get(tag), bacKey).execute()
  }

  private fun resetState() {
    scanPromise = null
    opts = null
    dg1File = null
    dg2File = null
  }

  private fun exceptionStack(exception: Throwable): String {
    val s = StringBuilder()
    val exceptionMsg = exception.message
    if (exceptionMsg != null) {
      s.append(exceptionMsg)
      s.append(" - ")
    }
    s.append(exception.javaClass.simpleName)
    val stack = exception.stackTrace

    if (stack.isNotEmpty()) {
      var count = 3
      var first = true
      var skip = false
      var file = ""
      s.append(" (")
      for (element in stack) {
        if (count > 0 && element.className.startsWith("io.tradle")) {
          if (!first) {
            s.append(" < ")
          } else {
            first = false
          }

          if (skip) {
            s.append("... < ")
            skip = false
          }

          if (file == element.fileName) {
            s.append("*")
          } else {
            file = element.fileName
            s.append(file.substring(0, file.length - 5)) // remove ".java"
            count -= 1
          }
          s.append(":").append(element.lineNumber)
        } else {
          skip = true
        }
      }
      if (skip) {
        if (!first) {
          s.append(" < ")
        }
        s.append("...")
      }
      s.append(")")
    }
    return s.toString()
  }

  private fun toBase64(bitmap: Bitmap, quality: Int): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return JPEG_DATA_URI_PREFIX + Base64.encodeToString(byteArray, Base64.NO_WRAP)
  }

  private inner class ReadTask(private val isoDep: IsoDep, private val bacKey: BACKeySpec?) : AsyncTask<Void, Void, Exception>() {

        private lateinit var dg1File: DG1File
        private lateinit var dg2File: DG2File
        private lateinit var dg14File: DG14File
        private lateinit var sodFile: SODFile
        private var imageBase64: String? = null
        private var bitmap: Bitmap? = null
        private var chipAuthSucceeded = false
        private var passiveAuthSuccess = false
        private lateinit var dg14Encoded: ByteArray

        override fun doInBackground(vararg params: Void?): Exception? {
            try {
                eventMessageEmitter(Messages.STOP_MOVING)
                isoDep.timeout = 20000
                eventMessageEmitter("Connecting to chip...")
                Log.e("MY_LOGS", "This should obvsly log")
                val cardService = try {
                    CardService.getInstance(isoDep)
                } catch (e: Exception) {
                  eventMessageEmitter("Failed to get CardService instance")
                    Log.e("MY_LOGS", "Failed to get CardService instance", e)
                    throw e
                }
                
                try {
                    cardService.open()
                } catch (e: Exception) {
                  eventMessageEmitter("Failed to open CardService")
                    Log.e("MY_LOGS", "Failed to open CardService", e)
                    isoDep.close()
                    Thread.sleep(500)
                    isoDep.connect()
                    cardService.open()
                }
                eventMessageEmitter("CardService opened")
                Log.e("MY_LOGS", "cardService opened")
                val service = PassportService(
                    cardService,
                    PassportService.NORMAL_MAX_TRANCEIVE_LENGTH * 2,
                    PassportService.DEFAULT_MAX_BLOCKSIZE * 2,
                    false,
                    false,
                )
                Log.e("MY_LOGS", "service gotten")
                eventMessageEmitter("Trying to get cardAccessFile...")
                service.open()
                Log.e("MY_LOGS", "service opened")
                eventMessageEmitter("CardService opened")
                var paceSucceeded = false
                try {
                  eventMessageEmitter("Trying to get cardAccessFile...")
                    Log.e("MY_LOGS", "trying to get cardAccessFile...")
                    val cardAccessFile = CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS))
                    Log.e("MY_LOGS", "cardAccessFile: ${cardAccessFile}")
                    eventMessageEmitter("CardAccessFile gotten")
                    val securityInfoCollection = cardAccessFile.securityInfos
                    for (securityInfo: SecurityInfo in securityInfoCollection) {
                        if (securityInfo is PACEInfo) {
                            Log.e("MY_LOGS", "trying PACE...")
                            eventMessageEmitter("Trying PACE...")
                            service.doPACE(
                                bacKey,
                                securityInfo.objectIdentifier,
                                PACEInfo.toParameterSpec(securityInfo.parameterId),
                                null,
                            )
                            Log.e("MY_LOGS", "PACE succeeded")
                            eventMessageEmitter("PACE succeeded")
                            paceSucceeded = true
                        }
                    }
                } catch (e: Exception) {
                  eventMessageEmitter("PACE failed")
                  eventMessageEmitter(e.message ?: "PACE failed")
                    Log.w("MY_LOGS", e)
                }
                eventMessageEmitter("Sending select applet command...")
                Log.e("MY_LOGS", "Sending select applet command with paceSucceeded: ${paceSucceeded}") // this is false so PACE doesn't succeed
                service.sendSelectApplet(paceSucceeded)

                if (!paceSucceeded) {
                    var bacSucceeded = false
                    var attempts = 0
                    val maxAttempts = 3
                    
                    while (!bacSucceeded && attempts < maxAttempts) {
                        try {
                            attempts++
                            eventMessageEmitter("Trying BAC attempt $attempts of $maxAttempts")
                            Log.e("MY_LOGS", "BAC attempt $attempts of $maxAttempts")
                            
                            if (attempts > 1) {
                                // Wait before retry
                                Thread.sleep(500)
                            }
                            
                            // Try to read EF_COM first
                            try {
                                service.getInputStream(PassportService.EF_COM).read()
                            } catch (e: Exception) {
                                // EF_COM failed, do BAC
                                service.doBAC(bacKey)
                            }
                            
                            bacSucceeded = true
                            eventMessageEmitter("BAC succeeded on attempt $attempts")
                            Log.e("MY_LOGS", "BAC succeeded on attempt $attempts")
                            
                        } catch (e: Exception) {
                          eventMessageEmitter("BAC attempt $attempts failed: ${e.message}")
                            Log.e("MY_LOGS", "BAC attempt $attempts failed: ${e.message}")
                            if (attempts == maxAttempts) {
                              eventMessageEmitter("BAC failed after $attempts attempts")
                                throw e // Re-throw on final attempt
                            }
                        }
                    }
                }

                
                val dg1In = service.getInputStream(PassportService.EF_DG1)
                dg1File = DG1File(dg1In)   
                val dg2In = service.getInputStream(PassportService.EF_DG2)
                dg2File = DG2File(dg2In)                
                val sodIn = service.getInputStream(PassportService.EF_SOD)
                sodFile = SODFile(sodIn)
                
                // val gson = Gson()
                // Log.d(TAG, "============FIRST CONSOLE LOG=============")
                // Log.d(TAG, "dg1File: " + gson.toJson(dg1File))
                // Log.d(TAG, "dg2File: " + gson.toJson(dg2File))
                // Log.d(TAG, "sodFile.docSigningCertificate: ${sodFile.docSigningCertificate}")
                // Log.d(TAG, "publicKey: ${sodFile.docSigningCertificate.publicKey}")
                // Log.d(TAG, "publicKey: ${sodFile.docSigningCertificate.publicKey.toString()}")
                // Log.d(TAG, "publicKey: ${sodFile.docSigningCertificate.publicKey.format}")
                // Log.d(TAG, "publicKey: ${Base64.encodeToString(sodFile.docSigningCertificate.publicKey.encoded, Base64.DEFAULT)}")
                // Log.d(TAG, "sodFile.docSigningCertificate: ${gson.toJson(sodFile.docSigningCertificate)}")
                // Log.d(TAG, "sodFile.dataGroupHashes: ${sodFile.dataGroupHashes}")
                // Log.d(TAG, "sodFile.dataGroupHashes: ${gson.toJson(sodFile.dataGroupHashes)}")
                // Log.d(TAG, "concatenated: $concatenated")
                // Log.d(TAG, "concatenated: ${gson.toJson(concatenated)}")
                // Log.d(TAG, "concatenated: ${gson.toJson(concatenated.joinToString("") { "%02x".format(it) })}")
                // Log.d(TAG, "sodFile.eContent: ${sodFile.eContent}")
                // Log.d(TAG, "sodFile.eContent: ${gson.toJson(sodFile.eContent)}")
                // Log.d(TAG, "sodFile.eContent: ${gson.toJson(sodFile.eContent.joinToString("") { "%02x".format(it) })}")
                // Log.d(TAG, "sodFile.encryptedDigest: ${sodFile.encryptedDigest}")
                // Log.d(TAG, "sodFile.encryptedDigest: ${gson.toJson(sodFile.encryptedDigest)}")
                // Log.d(TAG, "sodFile.encryptedDigest: ${gson.toJson(sodFile.encryptedDigest.joinToString("") { "%02x".format(it) })}")
                // var id = passportNumberView.text.toString()
                // try {
                //     postData(id, sodFile.eContent.joinToString("") { "%02x".format(it) }, sodFile.encryptedDigest.joinToString("") { "%02x".format(it) }, sodFile.docSigningCertificate.publicKey.toString())
                // } catch (e: IOException) {
                //     e.printStackTrace()
                // }
                // Log.d(TAG, "============LET'S VERIFY THE SIGNATURE=============")
                eventMessageEmitter(Messages.AUTH)
                doChipAuth(service)
                doPassiveAuth()

                // Log.d(TAG, "============SIGNATURE VERIFIED=============")
                // sendDataToJS(PassportData(dg1File, dg2File, sodFile))
                // Log.d(TAG, "============DATA SENT TO JS=============")

                val allFaceImageInfo: MutableList<FaceImageInfo> = ArrayList()
                dg2File.faceInfos.forEach {
                    allFaceImageInfo.addAll(it.faceImageInfos)
                }
                if (allFaceImageInfo.isNotEmpty()) {
                    val faceImageInfo = allFaceImageInfo.first()
                    val imageLength = faceImageInfo.imageLength
                    val dataInputStream = DataInputStream(faceImageInfo.imageInputStream)
                    val buffer = ByteArray(imageLength)
                    dataInputStream.readFully(buffer, 0, imageLength)
                    val inputStream: InputStream = ByteArrayInputStream(buffer, 0, imageLength)
                    bitmap = ImageUtil.decodeImage(appContext.reactContext, faceImageInfo.mimeType, inputStream)
                    imageBase64 = Base64.encodeToString(buffer, Base64.DEFAULT)
                }
            } catch (e: Exception) {
                eventMessageEmitter(Messages.RESET)
                eventMessageEmitter(e.message ?: "Error")
                return e
            }
            return null
        }

        private fun doChipAuth(service: PassportService) {
            try {
                val dg14In = service.getInputStream(PassportService.EF_DG14)
                dg14Encoded = IOUtils.toByteArray(dg14In)
                val dg14InByte = ByteArrayInputStream(dg14Encoded)
                dg14File = DG14File(dg14InByte)
                val dg14FileSecurityInfo = dg14File.securityInfos
                for (securityInfo: SecurityInfo in dg14FileSecurityInfo) {
                    if (securityInfo is ChipAuthenticationPublicKeyInfo) {
                        service.doEACCA(
                            securityInfo.keyId,
                            ChipAuthenticationPublicKeyInfo.ID_CA_ECDH_AES_CBC_CMAC_256,
                            securityInfo.objectIdentifier,
                            securityInfo.subjectPublicKey,
                        )
                        chipAuthSucceeded = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }

        private fun doPassiveAuth() {
            try {
              eventMessageEmitter("Starting passive authentication...")
                Log.d(TAG, "Starting passive authentication...")
                val digest = MessageDigest.getInstance(sodFile.digestAlgorithm)
                Log.d(TAG, "Using digest algorithm: ${sodFile.digestAlgorithm}")
                eventMessageEmitter("Reading data group hashes...")
                
                val dataHashes = sodFile.dataGroupHashes
                
                eventMessageEmitter("Reading DG14.....")
                val dg14Hash = if (chipAuthSucceeded) digest.digest(dg14Encoded) else ByteArray(0)
                eventMessageEmitter("Reading DG1.....")
                val dg1Hash = digest.digest(dg1File.encoded)
                eventMessageEmitter("Reading DG2.....")
                val dg2Hash = digest.digest(dg2File.encoded)
                
                // val gson = Gson()
                // Log.d(TAG, "dataHashes " + gson.toJson(dataHashes))
                // val hexMap = sodFile.dataGroupHashes.mapValues { (_, value) ->
                //     value.joinToString("") { "%02x".format(it) }
                // }
                // Log.d(TAG, "hexMap: ${gson.toJson(hexMap)}")
                // Log.d(TAG, "concatenated: $concatenated")
                // Log.d(TAG, "concatenated: ${gson.toJson(concatenated)}")
                // Log.d(TAG, "concatenated: ${gson.toJson(concatenated.joinToString("") { "%02x".format(it) })}")
                // Log.d(TAG, "dg1File.encoded " + gson.toJson(dg1File.encoded))
                // Log.d(TAG, "dg1File.encoded.joinToString " + gson.toJson(dg1File.encoded.joinToString("") { "%02x".format(it) }))
                // Log.d(TAG, "dg1Hash " + gson.toJson(dg1Hash))
                // Log.d(TAG, "dg1Hash.joinToString " + gson.toJson(dg1Hash.joinToString("") { "%02x".format(it) }))
                // Log.d(TAG, "dg2File.encoded " + gson.toJson(dg2File.encoded))
                // Log.d(TAG, "dg2File.encoded.joinToString " + gson.toJson(dg2File.encoded.joinToString("") { "%02x".format(it) }))
                // Log.d(TAG, "dg2Hash " + gson.toJson(dg2Hash))
                // Log.d(TAG, "dg2HashjoinToString " + gson.toJson(dg2Hash.joinToString("") { "%02x".format(it) }))

                Log.d(TAG, "Comparing data group hashes...")
                eventMessageEmitter(Messages.COMPARING)
                if (Arrays.equals(dg1Hash, dataHashes[1]) && Arrays.equals(dg2Hash, dataHashes[2])
                    && (!chipAuthSucceeded || Arrays.equals(dg14Hash, dataHashes[14]))) {

                    Log.d(TAG, "Data group hashes match.")

                    val asn1InputStream = ASN1InputStream(appContext?.reactContext?.assets?.open("masterList"))
                    val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
                    keystore.load(null, null)
                    val cf = CertificateFactory.getInstance("X.509")

                    var p: ASN1Primitive?
                    var obj = asn1InputStream.readObject()

                    while (obj != null) {
                        p = obj
                        val asn1 = ASN1Sequence.getInstance(p)
                        if (asn1 == null || asn1.size() == 0) {
                            throw IllegalArgumentException("Null or empty sequence passed.")
                        }

                        if (asn1.size() != 2) {
                            throw IllegalArgumentException("Incorrect sequence size: " + asn1.size())
                        }
                        val certSet = ASN1Set.getInstance(asn1.getObjectAt(1))
                        for (i in 0 until certSet.size()) {
                            val certificate = Certificate.getInstance(certSet.getObjectAt(i))
                            val pemCertificate = certificate.encoded
                            val javaCertificate = cf.generateCertificate(ByteArrayInputStream(pemCertificate))
                            keystore.setCertificateEntry(i.toString(), javaCertificate)
                        }
                        obj = asn1InputStream.readObject()

                    }

                    val docSigningCertificates = sodFile.docSigningCertificates
                    Log.d(TAG, "Checking document signing certificates for validity...")
                    eventMessageEmitter("Checking document signing certificates for validity...")
                    for (docSigningCertificate: X509Certificate in docSigningCertificates) {
                        docSigningCertificate.checkValidity()
                        Log.d(TAG, "Certificate: ${docSigningCertificate.subjectDN} is valid.")
                        eventMessageEmitter("Certificate: ${docSigningCertificate.subjectDN} is valid.")
                    }

                    val cp = cf.generateCertPath(docSigningCertificates)
                    val pkixParameters = PKIXParameters(keystore)
                    pkixParameters.isRevocationEnabled = false
                    val cpv = CertPathValidator.getInstance(CertPathValidator.getDefaultType())
                    Log.d(TAG, "Validating certificate path...")
                    eventMessageEmitter("Validating certificate path...")
                    cpv.validate(cp, pkixParameters)
                    var sodDigestEncryptionAlgorithm = sodFile.docSigningCertificate.sigAlgName
                    var isSSA = false
                    if ((sodDigestEncryptionAlgorithm == "SSAwithRSA/PSS")) {
                        sodDigestEncryptionAlgorithm = "SHA256withRSA/PSS"
                        isSSA = true

                    }
                    val sign = Signature.getInstance(sodDigestEncryptionAlgorithm)
                    if (isSSA) {
                        sign.setParameter(PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1))
                    }
                    sign.initVerify(sodFile.docSigningCertificate)
                    sign.update(sodFile.eContent)

                    passiveAuthSuccess = sign.verify(sodFile.encryptedDigest)
                    Log.d(TAG, "Passive authentication success: $passiveAuthSuccess")
                    eventMessageEmitter("Passive authentication success: $passiveAuthSuccess")
                }
            } catch (e: Exception) {
                eventMessageEmitter(Messages.RESET)
                eventMessageEmitter(e.message ?: "Error")
                Log.w(TAG, "Exception in passive authentication", e)
            }
        }

        override fun onPostExecute(result: Exception?) {
            if (scanPromise == null) return

            if (result != null) {
                // Log.w(TAG, exceptionStack(result))
                if (result is IOException) {
                    scanPromise?.reject("E_SCAN_FAILED_DISCONNECT", "Lost connection to chip on card",null)
                    eventMessageEmitter("Lost connection to chip on card")
              
                } else {
                  eventMessageEmitter("Failed to read data from chip")
                  eventMessageEmitter(result.message ?: "Error")
                    scanPromise?.reject("E_SCAN_FAILED", result.message, result)
                }

                resetState()
                return
            }

            val mrzInfo = dg1File.mrzInfo

            val gson = Gson()

            // val signedDataField = SODFile::class.java.getDeclaredField("signedData")
            // signedDataField.isAccessible = true
            
          //   val signedData = signedDataField.get(sodFile) as SignedData
            
            val eContentAsn1InputStream = ASN1InputStream(sodFile.eContent.inputStream())
          //   val eContentDecomposed: ASN1Primitive = eContentAsn1InputStream.readObject()
  
            val passport = Arguments.createMap()
            passport.putString("mrz", mrzInfo.toString())
            passport.putString("signatureAlgorithm", sodFile.docSigningCertificate.sigAlgName) // this one is new
            Log.d(TAG, "sodFile.docSigningCertificate: ${sodFile.docSigningCertificate}")
            eventMessageEmitter("sodFile.docSigningCertificate: ${sodFile.docSigningCertificate}")
            val certificate = sodFile.docSigningCertificate
            val certificateBytes = certificate.encoded
            val certificateBase64 = Base64.encodeToString(certificateBytes, Base64.DEFAULT)
            Log.d(TAG, "certificateBase64: ${certificateBase64}")
            eventMessageEmitter("certificateBase64: ${certificateBase64}")

            passport.putString("documentSigningCertificate", certificateBase64)
            eventMessageEmitter("documentSigningCertificate: ${certificateBase64}")
            val publicKey = sodFile.docSigningCertificate.publicKey
            if (publicKey is RSAPublicKey) {
                passport.putString("modulus", publicKey.modulus.toString())
            } else if (publicKey is ECPublicKey) {
              // Handle the elliptic curve public key case
              
              // val w = publicKey.getW()
              // passport.putString("publicKeyW", w.toString())
              
              // val ecParams = publicKey.getParams()
              // passport.putInt("cofactor", ecParams.getCofactor())
              // passport.putString("curve", ecParams.getCurve().toString())
              // passport.putString("generator", ecParams.getGenerator().toString())
              // passport.putString("order", ecParams.getOrder().toString())
              // if (ecParams is ECNamedCurveSpec) {
              //     passport.putString("curveName", ecParams.getName())
              // }
  
            //   Old one, probably wrong:
            //     passport.putString("curveName", (publicKey.parameters as ECNamedCurveSpec).name)
            //     passport.putString("curveName", (publicKey.parameters.algorithm)) or maybe this
                passport.putString("publicKeyQ", publicKey.q.toString())
            }

            passport.putString("dataGroupHashes", gson.toJson(sodFile.dataGroupHashes))
            passport.putString("eContent", gson.toJson(sodFile.eContent))
            passport.putString("encryptedDigest", gson.toJson(sodFile.encryptedDigest))

            // passport.putString("encapContentInfo", gson.toJson(sodFile.encapContentInfo))
            // passport.putString("contentInfo", gson.toJson(sodFile.contentInfo))
            passport.putString("digestAlgorithm", gson.toJson(sodFile.digestAlgorithm))
            passport.putString("signerInfoDigestAlgorithm", gson.toJson(sodFile.signerInfoDigestAlgorithm))
            passport.putString("digestEncryptionAlgorithm", gson.toJson(sodFile.digestEncryptionAlgorithm))
            passport.putString("LDSVersion", gson.toJson(sodFile.getLDSVersion()))
            passport.putString("unicodeVersion", gson.toJson(sodFile.unicodeVersion))


            // Get EncapContent (data group hashes) using reflection in Kotlin
            val getENC: Method = SODFile::class.java.getDeclaredMethod("getLDSSecurityObject", SignedData::class.java)
            getENC.isAccessible = true
            val signedDataField: Field = sodFile::class.java.getDeclaredField("signedData")
            signedDataField.isAccessible = true
            val signedData: SignedData = signedDataField.get(sodFile) as SignedData
            val ldsso: LDSSecurityObject = getENC.invoke(sodFile, signedData) as LDSSecurityObject

            passport.putString("encapContent", gson.toJson(ldsso.encoded))

            // Convert the document signing certificate to PEM format
            val docSigningCert = sodFile.docSigningCertificate
            val pemCert = "-----BEGIN CERTIFICATE-----\n" + Base64.encodeToString(docSigningCert.encoded, Base64.DEFAULT) + "-----END CERTIFICATE-----"
            passport.putString("documentSigningCertificate", pemCert)

            // passport.putString("getDocSigningCertificate", gson.toJson(sodFile.getDocSigningCertificate))
            // passport.putString("getIssuerX500Principal", gson.toJson(sodFile.getIssuerX500Principal))
            // passport.putString("getSerialNumber", gson.toJson(sodFile.getSerialNumber))
  
  
            // Another way to get signing time is to get into signedData.signerInfos, then search for the ICO identifier 1.2.840.113549.1.9.5 
            // passport.putString("signerInfos", gson.toJson(signedData.signerInfos))
            
            //   Log.d(TAG, "signedData.digestAlgorithms: ${gson.toJson(signedData.digestAlgorithms)}")
            //   Log.d(TAG, "signedData.signerInfos: ${gson.toJson(signedData.signerInfos)}")
            //   Log.d(TAG, "signedData.certificates: ${gson.toJson(signedData.certificates)}")
            
            var quality = 100
            val base64 = bitmap?.let { toBase64(it, quality) }
            val photo = Arguments.createMap()
            photo.putString("base64", base64 ?: "")
            photo.putInt("width", bitmap?.width ?: 0)
            photo.putInt("height", bitmap?.height ?: 0)
            passport.putMap("photo", photo)
            // passport.putString("dg2File", gson.toJson(dg2File))
            
            eventMessageEmitter(Messages.COMPLETED)
            scanPromise?.resolve(passport)
            eventMessageEmitter(Messages.RESET)
            resetState()
        }
  }


      private fun eventMessageEmitter(message: String) {
              sendEvent("onChange", mapOf(
                        "value" to message
                        ))
    }

    companion object {
        private val TAG = LetroPassportModule::class.java.simpleName
        private const val PARAM_DOC_NUM = "documentNumber";
        private const val PARAM_DOB = "dateOfBirth";
        private const val PARAM_DOE = "dateOfExpiry";
        private val E_ONE_REQ_AT_A_TIME = "E_ONE_REQ_AT_A_TIME"
        private val E_NOT_ENABLED = "E_NOT_ENABLED"
        private val E_NOT_SUPPORTED = "E_NOT_SUPPORTED"
        private val E_SCAN_CANCELED = "E_SCAN_CANCELED"
        const val JPEG_DATA_URI_PREFIX = "data:image/jpeg;base64,"
        private const val KEY_IS_SUPPORTED = "isSupported"
        private var instance: LetroPassportModule? = null

        fun getInstance(): LetroPassportModule {
            return instance ?: throw IllegalStateException("LetroPassportModule instance is not initialized")
        }
    }
}


data class Proof(
    val pi_a: List<String>,
    val pi_b: List<List<String>>,
    val pi_c: List<String>,
    val protocol: String,
    var curve: String = "bn128"
) {
    companion object {
        fun fromJson(jsonString: String): Proof {
            Log.d("Proof", jsonString)
            val json = Gson().fromJson(jsonString, Proof::class.java)
            json.curve = getDefaultCurve()
            return json
        }

        private fun getDefaultCurve(): String {
            return "bn128"
        }
    }
}

data class ZkProof(
    val proof: Proof,
    val pub_signals: List<String>
)

class ZKPTools(val context: Context) {
//   external fun witnesscalc_register_sha256WithRSAEncryption_65537(circuitBuffer: ByteArray,
//     circuitSize: Long,
//     jsonBuffer: ByteArray,
//     jsonSize: Long,
//     wtnsBuffer: ByteArray,
//     wtnsSize: LongArray,
//     errorMsg: ByteArray,
//     errorMsgMaxSize: Long): Int
//   external fun witnesscalc_disclose(circuitBuffer: ByteArray,
//     circuitSize: Long,
//     jsonBuffer: ByteArray,
//     jsonSize: Long,
//     wtnsBuffer: ByteArray,
//     wtnsSize: LongArray,
//     errorMsg: ByteArray,
//     errorMsgMaxSize: Long): Int
  external fun witnesscalc_prove_rsa_65537_sha256(circuitBuffer: ByteArray,
    circuitSize: Long,
    jsonBuffer: ByteArray,
    jsonSize: Long,
    wtnsBuffer: ByteArray,
    wtnsSize: LongArray,
    errorMsg: ByteArray,
    errorMsgMaxSize: Long): Int
  external fun witnesscalc_prove_rsa_65537_sha1(circuitBuffer: ByteArray,
    circuitSize: Long,
    jsonBuffer: ByteArray,
    jsonSize: Long,
    wtnsBuffer: ByteArray,
    wtnsSize: LongArray,
    errorMsg: ByteArray,
    errorMsgMaxSize: Long): Int
  external fun witnesscalc_prove_rsapss_65537_sha256(circuitBuffer: ByteArray,
    circuitSize: Long,
    jsonBuffer: ByteArray,
    jsonSize: Long,
    wtnsBuffer: ByteArray,
    wtnsSize: LongArray,
    errorMsg: ByteArray,
    errorMsgMaxSize: Long): Int
  external fun witnesscalc_vc_and_disclose(circuitBuffer: ByteArray,
    circuitSize: Long,
    jsonBuffer: ByteArray,
    jsonSize: Long,
    wtnsBuffer: ByteArray,
    wtnsSize: LongArray,
    errorMsg: ByteArray,
    errorMsgMaxSize: Long): Int

//   external fun witnesscalc_prove_ecdsa_secp256r1_sha1(circuitBuffer: ByteArray,
//     circuitSize: Long,
//     jsonBuffer: ByteArray,
//     jsonSize: Long,
//     wtnsBuffer: ByteArray,
//     wtnsSize: LongArray,
//     errorMsg: ByteArray,
//     errorMsgMaxSize: Long): Int
//   external fun witnesscalc_prove_ecdsa_secp256r1_sha256(circuitBuffer: ByteArray,
//     circuitSize: Long,
//     jsonBuffer: ByteArray,
//     jsonSize: Long,
//     wtnsBuffer: ByteArray,
//     wtnsSize: LongArray,
//     errorMsg: ByteArray,
//     errorMsgMaxSize: Long): Int
  external fun groth16_prover(
      zkeyBuffer: ByteArray, zkeySize: Long,
      wtnsBuffer: ByteArray, wtnsSize: Long,
      proofBuffer: ByteArray, proofSize: LongArray,
      publicBuffer: ByteArray, publicSize: LongArray,
      errorMsg: ByteArray, errorMsgMaxSize: Long
  ): Int
  external fun groth16_prover_zkey_file(
      zkeyPath: String,
      wtnsBuffer: ByteArray, wtnsSize: Long,
      proofBuffer: ByteArray, proofSize: LongArray,
      publicBuffer: ByteArray, publicSize: LongArray,
      errorMsg: ByteArray, errorMsgMaxSize: Long
  ): Int

  init {
      System.loadLibrary("rapidsnark");
      System.loadLibrary("expo.modules.letropassport")
  }

  fun openRawResourceAsByteArray(resourceName: Int): ByteArray {
      val inputStream = context.resources.openRawResource(resourceName)
      val byteArrayOutputStream = ByteArrayOutputStream()

      try {
          val buffer = ByteArray(1024)
          var length: Int

          while (inputStream.read(buffer).also { length = it } != -1) {
              byteArrayOutputStream.write(buffer, 0, length)
          }

          return byteArrayOutputStream.toByteArray()
      } finally {
          byteArrayOutputStream.close()
          inputStream.close()
      }
  }
}

class ZKPUseCase(val context: Context) {

    fun generateZKP(
        zkey_path: String,
        datBytes: ByteArray,
        inputs: ByteArray,
        proofFunction: (
            circuitBuffer: ByteArray,
            circuitSize: Long,
            jsonBuffer: ByteArray,
            jsonSize: Long,
            wtnsBuffer: ByteArray,
            wtnsSize: LongArray,
            errorMsg: ByteArray,
            errorMsgMaxSize: Long
        ) -> Int
    ): ZkProof {
        val zkpTool = ZKPTools(context)
        val datFile = datBytes

        val msg = ByteArray(256)

        val witnessLen = LongArray(1)
        witnessLen[0] = 100 * 1024 * 1024

        val byteArr = ByteArray(100 * 1024 * 1024)

        val res = proofFunction(
            datFile,
            datFile.size.toLong(),
            inputs,
            inputs.size.toLong(),
            byteArr,
            witnessLen,
            msg,
            256
        )

        Log.e("ZKPUseCase", "Witness gen res: $res")
        Log.e("ZKPUseCase", "Witness gen return length: ${byteArr.size}")

        if (res == 3) {
            throw Exception("Error 3")
        }

        if (res == 2) {
            throw Exception("Not enough memory for zkp")
        }

        if (res == 1) {
            throw Exception("Error during zkp ${msg.decodeToString()}")
        }

        val pubData = ByteArray(4 *1024 *1024)
        val pubLen = LongArray(1)
        pubLen[0] = pubData.size.toLong()

        val proofData = ByteArray(4*1024*1024)
        val proofLen = LongArray(1)
        proofLen[0] = proofData.size.toLong()

        val witnessData = byteArr.copyOfRange(0, witnessLen[0].toInt())

        Log.e("ZKPUseCase", "zkey_path: $zkey_path")

        val proofGen = zkpTool.groth16_prover_zkey_file(
            zkey_path,
            witnessData,
            witnessLen[0],
            proofData,
            proofLen,
            pubData,
            pubLen,
            msg,
            256
        )

        Log.e("ZKPUseCase", "proofGen res: $proofGen")

        if (proofGen == 2) {
            throw Exception("Not enough memory for proofGen ${msg.decodeToString()}")
        }

        if (proofGen == 1) {
            throw Exception("Error during proofGen ${msg.decodeToString()}")
        }

        val proofDataZip = proofData.copyOfRange(0, proofLen[0].toInt())

        val index = findLastIndexOfSubstring(
            proofDataZip.toString(Charsets.UTF_8),
            "\"protocol\":\"groth16\"}"
        )
        val indexPubData = findLastIndexOfSubstring(
            pubData.decodeToString(),
            "]"
        )

        val formattedPubData = pubData.decodeToString().slice(0..indexPubData)

        val formattedProof = proofDataZip.toString(Charsets.UTF_8).slice(0..index)

        Log.e("ZKPUseCase", "formattedProof: $formattedProof")

        val proof = Proof.fromJson(formattedProof)

        Log.e("ZKPUseCase", "Proof: $proof")

        return ZkProof(
            proof = proof,
            pub_signals = getPubSignals(formattedPubData).toList()
        )
    }

    private fun findLastIndexOfSubstring(mainString: String, searchString: String): Int {
        val index = mainString.lastIndexOf(searchString)

        if (index != -1) {
            // If substring is found, calculate the last index of the substring
            return index + searchString.length - 1
        }

        return -1
    }

    private fun getPubSignals(jsonString: String): List<String> {
        val gson = Gson()
        val stringArray = gson.fromJson(jsonString, Array<String>::class.java)
        return stringArray.toList()
    }
}