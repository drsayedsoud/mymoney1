package com.example.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun parseFinancialPrompt(
        prompt: String,
        apiKey: String,
        customInstructions: String,
        customCategories: String,
        currentTransactions: List<Transaction>
    ): GeminiResult {
        if (apiKey.isEmpty()) {
            return GeminiResult.Error("مفتاح API الخاص بـ Gemini غير متوفر. يرجى إضافته في الإعدادات.")
        }

        // Format existing transactions as context for Gemini
        val transactionsContext = StringBuilder()
        if (currentTransactions.isEmpty()) {
            transactionsContext.append("لا توجد أي معاملات سابقة بعد.")
        } else {
            transactionsContext.append("قائمة المعاملات الحالية:\n")
            currentTransactions.take(30).forEach { tx ->
                val typeAr = if (tx.type == "INCOME") "دخل" else "منصرف"
                transactionsContext.append("- مذكر معرف/رقم #${tx.id}: بقيمة ${tx.amount} EGP، من فئة ${tx.category}، نوع: $typeAr، التفاصيل: ${tx.description}\n")
            }
        }

        val baseSystemInstruction = """
أنت مساعد مالي شخصي ذكي (وكيل ذكاء اصطناعي صوتي) يدعى "المدير المالي الذكي".
مهمتك الأساسية هي فهم وتحليل الأوامر الصوتية بالكامل باللهجة العامية المصرية والرد بالعامية المصرية المناسبة للنطق الصوتي (Text-to-Speech).
يجب عليك الرد بصيغة JSON نظيفة فقط (بدون أي كود أو إشارات زخرفية مثل ```json أو ```).
يجب أن تحتوي بنية ملف الـ JSON بدقة على الحقول التالية:
{
  "action": "ADD" أو "DELETE" أو "REPORT" أو "CHAT",
  "transactionToAdd": {
    "amount": رقم_عشري,
    "type": "INCOME" أو "EXPENSE",
    "category": "فئة_المعاملة_الموافقة_للمعاملة_من_خلال_قائمة_التصنيفات_أدناه",
    "description": "تفاصيل المعاملة بالعامية المصرية"
  },
  "transactionIdToDelete": رقم_معرف_المعاملة_الملغاة_أو_المحذوفة_إن_وجدت,
  "confirmationMessage": "رسالة تأكيد مبهجة ومختصرة جداً باللهجة العامية المصرية تشرح الإجراء الذي اتخذته والتصنيف الذي اخترته بصوت ودود (مثال: 'تمام يا فندم، سجلتلك 50 جنيه مواصلات')",
  "generalResponseText": "تقرير مالي مقتضب أو نصيحة أو رد ذكي باللهجة العامية المصرية الودية يناسب النطق الصوتي مباشرة"
}

قائمة تصنيفات الفئات المخصصة المتاحة حاليًا بالتطبيق:
$customCategories

دليل ترجمة وفهم اللهجة العامية المصرية للأوامر المالية:
- الكلمات الدالة على المصاريف (EXPENSE): "صرفت"، "دفعت"، "جبت"، "شحنت رصيد/كهربا"، "طيرت"، "اشتريت"، "كلفني"، "حاسبت على".
- الكلمات الدالة على الدخل (INCOME): "قبضت"، "جالي"، "دخل عيادتي/محلي"، "كسبت"، "أخذت/خدت"، "رجّعت"، "استرديت".
- الكلمات الدالة على الحذف والتعديل (DELETE): "امسح"، "احذف"، "شيلي"، "طير"، "لغي".
- الكلمات الدالة على التقارير والرسومات (REPORT): "اديني تقرير"، "عايز تقرير"، "احسبلي"، "قولي التقرير"، "فرجني على الرسم البياني/الرسمة"، "قولي الوضع المالي إيه".

إرشادات وتوجيهات التصنيف هامة جداً:
- يجب تصنيف أي معاملة ADD بأفضل فئة مطابقة في القائمة المخصصة أعلاه.
- الرد دائمًا بالعامية المصرية الودية والسهلة في النطق عبر المذياع.

تعليمات المستخدم الإضافية للتطبيق:
$customInstructions

سياق المعاملات الحالية لمساعدتك في اتخاذ قرارات الحذف أو صياغة التقارير:
$transactionsContext
        """.trimIndent()

        // Build Gemini Direct REST request payload
        val requestJson = JSONObject()
        
        // Content details
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        requestJson.put("contents", contentsArray)

        // System instructions format
        val systemInstructionObj = JSONObject()
        val systemPartsArray = JSONArray()
        val systemPartObj = JSONObject()
        systemPartObj.put("text", baseSystemInstruction)
        systemPartsArray.put(systemPartObj)
        systemInstructionObj.put("parts", systemPartsArray)
        requestJson.put("systemInstruction", systemInstructionObj)

        // Generation Config for JSON response
        val generationConfigObj = JSONObject()
        val responseFormatObj = JSONObject()
        responseFormatObj.put("mimeType", "application/json")
        generationConfigObj.put("responseFormat", responseFormatObj)
        generationConfigObj.put("temperature", 0.3) // Lower temperature for structured output stability
        requestJson.put("generationConfig", generationConfigObj)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e("GeminiService", "API call failed with code ${response.code}: $errBody")
                return GeminiResult.Error("فشل الاتصال بخدمة الذكاء الاصطناعي (رمز ${response.code}). يرجى التحقق من مفتاح الـ API.")
            }

            val responseBody = response.body?.string() ?: return GeminiResult.Error("رد فارغ من الاستجابة.")
            Log.d("GeminiService", "Raw Response: $responseBody")

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return GeminiResult.Error("لم يتم العثور على رد أو اقتراح مالي مناسب.")
            }

            val textPart = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .optString("text")

            if (textPart.isNullOrEmpty()) {
                return GeminiResult.Error("تعذر قراءة الجواب من خادم جمناي.")
            }

            // Parse clean JSON text result
            val cleanJsonString = textPart.trim()
            val parsedResult = JSONObject(cleanJsonString)

            return GeminiResult.Success(parsedResult)

        } catch (e: Exception) {
            Log.e("GeminiService", "Exception during parsing", e)
            return GeminiResult.Error("حدث خطأ أثناء معالجة البيانات ماليًا: ${e.localizedMessage}")
        }
    }
}

sealed class GeminiResult {
    data class Success(val response: JSONObject) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}
