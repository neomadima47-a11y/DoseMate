package com.example.util

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    ISIZULU("zu", "isiZulu", "isiZulu"),
    SESOTHO("st", "Sesotho", "Sesotho"),
    AFRIKAANS("af", "Afrikaans", "Afrikaans"),
    SEPEDI("nso", "Sepedi", "Sepedi"),
    SETSWANA("tn", "Setswana", "Setswana"),
    ISIXHOSA("xh", "isiXhosa", "isiXhosa")
}

object Strings {
    private val englishMap = mapOf(
        "app_tagline" to "Your health, on your terms",
        "login" to "Log In",
        "create_account" to "Create account",
        "register" to "Register",
        "continue_google" to "Continue with Google",
        "email" to "Email",
        "password" to "Password",
        "full_name" to "Full name",
        "preferred_language" to "Preferred language",
        "dashboard" to "Dashboard",
        "medications" to "Medications",
        "log_reading" to "Log reading",
        "find_clinic" to "Find a clinic",
        "settings" to "Settings",
        "today_medication" to "Today's medication",
        "health_log" to "Health log",
        "saved_offline" to "saved offline, will sync",
        "add_medication" to "Add medication",
        "medication_name" to "Medication name",
        "dosage" to "Dosage",
        "frequency" to "Frequency",
        "reminder_times" to "Reminder times",
        "save_medication" to "Save medication",
        "save_reading" to "Save reading",
        "adherence" to "This week's adherence",
        "blood_sugar" to "Blood sugar",
        "blood_pressure" to "Blood pressure",
        "symptom" to "Symptom",
        "when_taken" to "When taken",
        "notes" to "Notes (optional)",
        "offline_save_notice" to "Will save on this device and sync automatically",
        "nearby_clinics" to "Nearby clinics",
        "get_directions" to "Get directions",
        "online_locator" to "Online-only clinic locator",
        "weather_tip" to "Hot day — store insulin below 25°C & stay hydrated",
        "medication_reminders" to "Medication reminders",
        "dark_mode" to "Dark mode",
        "change_password" to "Change password",
        "logout" to "Log out"
    )

    private val isizuluMap = mapOf(
        "app_tagline" to "Impilo yakho, ngemibandela yakho",
        "login" to "Ngena",
        "create_account" to "Dala i-akhawunti",
        "register" to "Bhalisa",
        "continue_google" to "Qhubeka nge-Google",
        "email" to "I-meyili",
        "password" to "Inkomba-fihlo",
        "full_name" to "Igama eliphelele",
        "preferred_language" to "Ulimi olukhethiwe",
        "dashboard" to "Ikhaya",
        "medications" to "Imithi",
        "log_reading" to "Bhalisa isilinganiso",
        "find_clinic" to "Thola ikliniki",
        "settings" to "Izilungiselelo",
        "today_medication" to "Imithi yanamuhla",
        "health_log" to "Ilogi yezempilo",
        "saved_offline" to "ikulondolozwe ngaphandle kwe-inthanethi",
        "add_medication" to "Faka umuthi",
        "medication_name" to "Igama lomuthi",
        "dosage" to "Isilinganiso",
        "frequency" to "Izikhathi",
        "reminder_times" to "Izikhathi zesikhumbuzo",
        "save_medication" to "Londoloza umuthi",
        "save_reading" to "Londoloza isilinganiso",
        "adherence" to "Ukuthatha imithi kule wiki",
        "blood_sugar" to "Ushukela egazini",
        "blood_pressure" to "Ufutho lwegazi",
        "symptom" to "Izimpawu",
        "when_taken" to "Ithathe nini",
        "notes" to "Amanothi",
        "offline_save_notice" to "Kuzosindiswa kuyo le divaysi bese kuhlanganiswa ngokuzenzakalelayo",
        "nearby_clinics" to "Amakliniki aseduze",
        "get_directions" to "Thola izindlela",
        "online_locator" to "Imephu yekliniki ye-inthanethi",
        "weather_tip" to "Liyashisa namuhla — gcina i-insulin ngaphansi kuka-25°C",
        "medication_reminders" to "Izikhumbuzo zemithi",
        "dark_mode" to "Imo emnyama",
        "change_password" to "Shintsha iphasiwedi",
        "logout" to "Phuma"
    )

    private val sesothoMap = mapOf(
        "app_tagline" to "Bophelo ba hao, ka melao ya hao",
        "login" to "Kena",
        "create_account" to "Etsa akhaonto",
        "register" to "Ingolise",
        "continue_google" to "Tswela pele ka Google",
        "email" to "Imeile",
        "password" to "Lekunutu",
        "full_name" to "Lebitso ka botlalo",
        "preferred_language" to "Puo e kgethilweng",
        "dashboard" to "Legae",
        "medications" to "Meriana",
        "log_reading" to "Nka sekala",
        "find_clinic" to "Batla tleliniki",
        "settings" to "Disetting",
        "today_medication" to "Meriana ya kajeno",
        "health_log" to "Buka ya bophelo",
        "saved_offline" to "e bolokilwe offline",
        "add_medication" to "Otsa moriana",
        "medication_name" to "Lebitso la moriana",
        "dosage" to "Tekanyo",
        "frequency" to "Kgafetsa",
        "reminder_times" to "Dinako tsa kgopotso",
        "save_medication" to "Boloka moriana",
        "save_reading" to "Boloka sekala",
        "adherence" to "Tiriso ya moriana beke ena",
        "blood_sugar" to "Madi a tsoekere",
        "blood_pressure" to "Kgotlelo ya madi",
        "symptom" to "Matshwao",
        "when_taken" to "E tserwe neng",
        "notes" to "Dintlha",
        "offline_save_notice" to "E tla bolokwa sesebelisueng sena mme e sync ka ho iketsahalla",
        "nearby_clinics" to "Ditleliniki tse haufi",
        "get_directions" to "Fumanang ditaelo tsa tsela",
        "online_locator" to "Mmapa wa inthanete wa tleliniki",
        "weather_tip" to "Letsatsi le chesang — boloka insulin tlas'a 25°C",
        "medication_reminders" to "Dikgopotso tsa meriana",
        "dark_mode" to "Monepo wa lefifi",
        "change_password" to "Fetola password",
        "logout" to "Tswa"
    )

    private val afrikaansMap = mapOf(
        "app_tagline" to "Jou gesondheid, op jou terme",
        "login" to "Teken in",
        "create_account" to "Skep rekening",
        "register" to "Registreer",
        "continue_google" to "Gaan voort met Google",
        "email" to "E-pos",
        "password" to "Wagwoord",
        "full_name" to "Volle naam",
        "preferred_language" to "Voorkeurtaal",
        "dashboard" to "Tuisblad",
        "medications" to "Medikasie",
        "log_reading" to "Teken lesing aan",
        "find_clinic" to "Vind kliniek",
        "settings" to "Instellings",
        "today_medication" to "Vandag se medikasie",
        "health_log" to "Gesondheidslogboek",
        "saved_offline" to "vanlyn gestoor, sal sinchroniseer",
        "add_medication" to "Voeg medikasie by",
        "medication_name" to "Medikasie naam",
        "dosage" to "Dosis",
        "frequency" to "Frekwensie",
        "reminder_times" to "Herinneringstye",
        "save_medication" to "Stoor medikasie",
        "save_reading" to "Stoor lesing",
        "adherence" to "Hierdie week se nakoming",
        "blood_sugar" to "Bloedsuiker",
        "blood_pressure" to "Bloeddruk",
        "symptom" to "Simptoom",
        "when_taken" to "Wanneer geneem",
        "notes" to "Notas (opsioneel)",
        "offline_save_notice" to "Sal op hierdie toestel gestoor word en outomaties sinchroniseer",
        "nearby_clinics" to "Nabygeleë klinieke",
        "get_directions" to "Kry aanwysings",
        "online_locator" to "Aanlyn kliniek gids",
        "weather_tip" to "Warm dag — stoor insulien onder 25°C",
        "medication_reminders" to "Medikasie herinneringe",
        "dark_mode" to "Donker tema",
        "change_password" to "Verander wagwoord",
        "logout" to "Meld af"
    )

    private val sepediMap = mapOf(
        "app_tagline" to "Bophelo bja gago, ka melao ya gago",
        "login" to "Tsena",
        "create_account" to "Hlama akhaonte",
        "register" to "Iingolise",
        "continue_google" to "Tswela pele ka Google",
        "email" to "Imeile",
        "password" to "Lekunutu",
        "full_name" to "Leina ka botlalo",
        "preferred_language" to "Polelo ye e kgethilwego",
        "dashboard" to "Gae",
        "medications" to "Dihlare",
        "log_reading" to "Gatisa sekala",
        "find_clinic" to "Seka tleleniki",
        "settings" to "Dihlopho",
        "today_medication" to "Dihlare tsa lehono",
        "health_log" to "Puku ya bophelo",
        "saved_offline" to "e bolokilwe ntle le inthanete",
        "add_medication" to "Tsenya sehlare",
        "medication_name" to "Leina la sehlare",
        "dosage" to "Tekanyo",
        "frequency" to "Makga",
        "reminder_times" to "Dinako tsa kgopotso",
        "save_medication" to "Boloka sehlare",
        "save_reading" to "Boloka sekala",
        "adherence" to "Tiriso ya sehlare beke ye",
        "blood_sugar" to "Madi a swiri",
        "blood_pressure" to "Kgotlelelo ya madi",
        "symptom" to "Dika",
        "when_taken" to "E tserwe neng",
        "notes" to "Dintlha",
        "offline_save_notice" to "E tla bolokwa mo founong mme ya sync ka go iketsagalela",
        "nearby_clinics" to "Ditleleniki tsa kgauswi",
        "get_directions" to "Kopa ditaelo tsa tsela",
        "online_locator" to "Mmapa wa inthanete wa tleleniki",
        "weather_tip" to "Letsatshi le le fisang — boloka insulin tlase ga 25°C",
        "medication_reminders" to "Dikgopotso tsa dihlare",
        "dark_mode" to "Monepo wa lefifi",
        "change_password" to "Phetola lekunutu",
        "logout" to "Tswa"
    )

    private val setswanaMap = mapOf(
        "app_tagline" to "Botsogo jwa gago, ka melao ya gago",
        "login" to "Kena",
        "create_account" to "Dira akhaonto",
        "register" to "Ikwalise",
        "continue_google" to "Tswelela ka Google",
        "email" to "Emeile",
        "password" to "Lekwalo la sephiri",
        "full_name" to "Leina ka botlalo",
        "preferred_language" to "Puo e e elediwang",
        "dashboard" to "Legae",
        "medications" to "Meriana",
        "log_reading" to "Kwala sekale",
        "find_clinic" to "Batla tleleniki",
        "settings" to "Di-setting",
        "today_medication" to "Meriana ya gompieno",
        "health_log" to "Pekano ya botsogo",
        "saved_offline" to "e bolokilwe ntle le inthanete",
        "add_medication" to "Tsenya molemo",
        "medication_name" to "Leina la molemo",
        "dosage" to "Tekanyo",
        "frequency" to "Kgafetsa",
        "reminder_times" to "Dinako tsa kgopotso",
        "save_medication" to "Boloka molemo",
        "save_reading" to "Boloka sekale",
        "adherence" to "Tiriso ya molemo beke e",
        "blood_sugar" to "Madi a mantsi/sukiri",
        "blood_pressure" to "Kgotlelo ya madi",
        "symptom" to "Matshwao",
        "when_taken" to "E tserwe leng",
        "notes" to "Dintlha (fa o batla)",
        "offline_save_notice" to "E tla bolokwa mo founong e bo e sync fela fa inthanete e bowa",
        "nearby_clinics" to "Ditleleniki tse di gaufi",
        "get_directions" to "Kopa ditaelo tsa tsela",
        "online_locator" to "Mmapa wa inthanete wa tleleniki",
        "weather_tip" to "Letsatsi le le tsididi/gabo — boloka insulin fa tlase ga 25°C",
        "medication_reminders" to "Dikgopotso tsa meriana",
        "dark_mode" to "Monepo wa lefifi",
        "change_password" to "Phetola lekwalo la sephiri",
        "logout" to "Tswaa"
    )

    private val isixhosaMap = mapOf(
        "app_tagline" to "Impilo yakho, ngokwemigaqo yakho",
        "login" to "Ngena",
        "create_account" to "Cala i-akhawunti",
        "register" to "Bhalisa",
        "continue_google" to "Qhubeka nge-Google",
        "email" to "I-imeyile",
        "password" to "Ikhodi yokungena",
        "full_name" to "Igama elipheleleyo",
        "preferred_language" to "Ulwimi olukhethiweyo",
        "dashboard" to "Ikhaya",
        "medications" to "Amiyeza",
        "log_reading" to "Bhala umlinganiselo",
        "find_clinic" to "Fumana ikliniki",
        "settings" to "Izilungiselelo",
        "today_medication" to "Amiyeza wanamhla",
        "health_log" to "Ilogi yempilo",
        "saved_offline" to "igcinwe ngaphandle kwe-intanethi",
        "add_medication" to "Faka umyeza",
        "medication_name" to "Igama lomyeza",
        "dosage" to "Iskali",
        "frequency" to "Amaxesha",
        "reminder_times" to "Amaxesha esikhumbuzo",
        "save_medication" to "Gcina umyeza",
        "save_reading" to "Gcina umlinganiselo",
        "adherence" to "Ukuthatha amiyeza kule veki",
        "blood_sugar" to "Isonka somhlaba egazini",
        "blood_pressure" to "Ufutho lwegazi",
        "symptom" to "Izingqisho",
        "when_taken" to "Ithathwe nini",
        "notes" to "Amanqaku",
        "offline_save_notice" to "Iza kugcinwa kule divayisi kwaye isincronise ngokuzenzekelayo",
        "nearby_clinics" to "Amakliniki akufutshane",
        "get_directions" to "Fumana izikhokelo",
        "online_locator" to "Imephu yekliniki ye-intanethi",
        "weather_tip" to "Kushushu namhlanje — gcina i-insulin ngaphantsi kuka-25°C",
        "medication_reminders" to "Izikhumbuzo zemiyeza",
        "dark_mode" to "Imo emnyama",
        "change_password" to "Tshintsha iphasiwedi",
        "logout" to "Phuma"
    )

    fun get(key: String, languageName: String = "English"): String {
        val map = when {
            languageName.equals("isiZulu", ignoreCase = true) -> isizuluMap
            languageName.equals("Sesotho", ignoreCase = true) -> sesothoMap
            languageName.equals("Afrikaans", ignoreCase = true) -> afrikaansMap
            languageName.equals("Sepedi", ignoreCase = true) -> sepediMap
            languageName.equals("Setswana", ignoreCase = true) -> setswanaMap
            languageName.equals("isiXhosa", ignoreCase = true) -> isixhosaMap
            else -> englishMap
        }
        return map[key] ?: englishMap[key] ?: key
    }

    fun translatePhrase(phrase: String, languageName: String = "English"): String {
        val trimmed = phrase.trim()
        if (trimmed.isEmpty()) return phrase

        val directTranslation = get(trimmed, languageName)
        if (directTranslation != trimmed && directTranslation.isNotBlank()) {
            return directTranslation
        }

        val phraseMap = mapOf(
            "Dashboard" to mapOf("isiZulu" to "Ikhaya", "Sesotho" to "Legae", "Afrikaans" to "Tuisblad", "Sepedi" to "Gae", "Setswana" to "Legae", "isiXhosa" to "Ikhaya"),
            "Log Reading" to mapOf("isiZulu" to "Bhalisa Isilinganiso", "Sesotho" to "Nka Sekala", "Afrikaans" to "Teken Lesing Aan", "Sepedi" to "Gatisa Sekala", "Setswana" to "Kwala Sekale", "isiXhosa" to "Bhala Umlinganiselo"),
            "Medications" to mapOf("isiZulu" to "Imithi", "Sesotho" to "Meriana", "Afrikaans" to "Medikasie", "Sepedi" to "Dihlare", "Setswana" to "Meriana", "isiXhosa" to "Amiyeza"),
            "History" to mapOf("isiZulu" to "Umlando", "Sesotho" to "Ntlha ya Nakong", "Afrikaans" to "Geskiedenis", "Sepedi" to "Nako ye e fetilego", "Setswana" to "Ditiragalo", "isiXhosa" to "Imbali"),
            "Clinics" to mapOf("isiZulu" to "Amakliniki", "Sesotho" to "Ditleliniki", "Afrikaans" to "Klinieke", "Sepedi" to "Ditleleniki", "Setswana" to "Ditleleniki", "isiXhosa" to "Amakliniki"),
            "Settings" to mapOf("isiZulu" to "Izilungiselelo", "Sesotho" to "Disetting", "Afrikaans" to "Instellings", "Sepedi" to "Dihlopho", "Setswana" to "Di-setting", "isiXhosa" to "Izilungiselelo"),
            "Sign In" to mapOf("isiZulu" to "Ngena", "Sesotho" to "Kena", "Afrikaans" to "Teken in", "Sepedi" to "Tsena", "Setswana" to "Kena", "isiXhosa" to "Ngena"),
            "Register" to mapOf("isiZulu" to "Bhalisa", "Sesotho" to "Ingolise", "Afrikaans" to "Registreer", "Sepedi" to "Iingolise", "Setswana" to "Ikwalise", "isiXhosa" to "Bhalisa"),
            "Full Name" to mapOf("isiZulu" to "Igama Eliphelele", "Sesotho" to "Lebitso Ka Botlalo", "Afrikaans" to "Volle Naam", "Sepedi" to "Leina Ka Botlalo", "Setswana" to "Leina Ka Botlalo", "isiXhosa" to "Igama Elipheleleyo"),
            "Email Address" to mapOf("isiZulu" to "I-meyili", "Sesotho" to "Imeile", "Afrikaans" to "E-pos Adres", "Sepedi" to "Imeile", "Setswana" to "Emeile", "isiXhosa" to "I-imeyile"),
            "Password / PIN" to mapOf("isiZulu" to "Inkomba-fihlo", "Sesotho" to "Lekunutu", "Afrikaans" to "Wagwoord", "Sepedi" to "Lekunutu", "Setswana" to "Lekwalo La Sephiri", "isiXhosa" to "Ikhodi Yokungena"),
            "Sign In to DoseMate" to mapOf("isiZulu" to "Ngena ku-DoseMate", "Sesotho" to "Kena ho DoseMate", "Afrikaans" to "Teken in by DoseMate", "Sepedi" to "Tsena go DoseMate", "Setswana" to "Kena mo DoseMate", "isiXhosa" to "Ngena ku-DoseMate"),
            "Create DoseMate Account" to mapOf("isiZulu" to "Dala I-akhawunti ye-DoseMate", "Sesotho" to "Etsa Akhaonto ya DoseMate", "Afrikaans" to "Skep DoseMate Rekening", "Sepedi" to "Hlama Akhaonte ya DoseMate", "Setswana" to "Dira Akhaonto ya DoseMate", "isiXhosa" to "Cala I-akhawunti ye-DoseMate"),
            "Sign in with Google" to mapOf("isiZulu" to "Qhubeka nge-Google", "Sesotho" to "Tswela pele ka Google", "Afrikaans" to "Gaan voort met Google", "Sepedi" to "Tswela pele ka Google", "Setswana" to "Tswelela ka Google", "isiXhosa" to "Qhubeka nge-Google"),
            "All Doses" to mapOf("isiZulu" to "Yonke Imithi", "Sesotho" to "Meriana Ka Moka", "Afrikaans" to "Alle Dosisse", "Sepedi" to "Dihlare Ka Moka", "Setswana" to "Meriana Yotlhe", "isiXhosa" to "Yonge Amiyeza"),
            "Due Today" to mapOf("isiZulu" to "Efanele Namuhla", "Sesotho" to "E tswanelang Kajeno", "Afrikaans" to "Vandag Verskuldig", "Sepedi" to "Mohlaleng wa Lehono", "Setswana" to "E e tshwanetseng Gompieno", "isiXhosa" to "Efanelayo Wanamhla"),
            "Completed" to mapOf("isiZulu" to "Iqediwe", "Sesotho" to "E qetilwe", "Afrikaans" to "Voltooi", "Sepedi" to "E feditswe", "Setswana" to "E feditswe", "isiXhosa" to "Iqukunjelwe"),
            "Prescription Regimen" to mapOf("isiZulu" to "Uhlelo Lwemithi", "Sesotho" to "Mekgwa ya Meriana", "Afrikaans" to "Medikasie Skedule", "Sepedi" to "Dithulaganyo tsa Dihlare", "Setswana" to "Mekgwa ya Meriana", "isiXhosa" to "Inqubo Yemiyeza"),
            "Today's Medications" to mapOf("isiZulu" to "Imithi Yanamuhla", "Sesotho" to "Meriana ya Kajeno", "Afrikaans" to "Vandag se Medikasie", "Sepedi" to "Dihlare tsa Lehono", "Setswana" to "Meriana ya Gompieno", "isiXhosa" to "Amiyeza Wanamhla"),
            "Health Summary" to mapOf("isiZulu" to "Isifinyezo Sempilo", "Sesotho" to "Kgutshwfalo ya Bophelo", "Afrikaans" to "Gesondheidsopsomming", "Sepedi" to "Kakaretso ya Bophelo", "Setswana" to "Tshobokanyo ya Botsogo", "isiXhosa" to "Isishwankathelo Sempilo"),
            "Recent Health Log" to mapOf("isiZulu" to "Ilogi Yempilo Yakamuva", "Sesotho" to "Buka ya Bophelo e Ntjha", "Afrikaans" to "Onlangse Gesondheidslogboek", "Sepedi" to "Puku ya Bophelo ya Bjale", "Setswana" to "Pekano ya Botsogo ya Sesweng", "isiXhosa" to "Ilogi Yempilo Yamva"),
            "Adherence Tracker" to mapOf("isiZulu" to "Umlandeli Wokuthatha Imithi", "Sesotho" to "Mohetsi wa ho Nka Moriana", "Afrikaans" to "Nakomingspoorder", "Sepedi" to "Molatedi wa go Tsea Sehlare", "Setswana" to "Molateli wa go Tsea Molemo", "isiXhosa" to "Umkhokheli Wokuthatha Amiyeza"),
            "Log Vitals" to mapOf("isiZulu" to "Bhalisa Isilinganiso", "Sesotho" to "Nka Sekala", "Afrikaans" to "Teken Lesing Aan", "Sepedi" to "Gatisa Sekala", "Setswana" to "Kwala Sekale", "isiXhosa" to "Bhala Umlinganiselo"),
            "Consult AI" to mapOf("isiZulu" to "Thintana ne-AI", "Sesotho" to "Buisana le AI", "Afrikaans" to "Raadpleeg KAI", "Sepedi" to "Rera le AI", "Setswana" to "Ikgokaganye le AI", "isiXhosa" to "Dibana ne-AI"),
            "Daily Dosage Regimen" to mapOf("isiZulu" to "Izilinganiso Zansuku Zonke", "Sesotho" to "Tekanyo ya Letsatsi le Letsatsi", "Afrikaans" to "Daaglikse Dosis Skedule", "Sepedi" to "Tekanyo ya Tšatši le Tšatši", "Setswana" to "Tekanyo ya Tsatsi le Tsatsi", "isiXhosa" to "Iskali Semiaxesha Yonke"),
            "Add Medication" to mapOf("isiZulu" to "Faka Umuthi", "Sesotho" to "Otsa Moriana", "Afrikaans" to "Voeg Medikasie By", "Sepedi" to "Tsenya Sehlare", "Setswana" to "Tsenya Molemo", "isiXhosa" to "Faka Umyeza"),
            "Take Dose" to mapOf("isiZulu" to "Thatha Umuthi", "Sesotho" to "Nka Moriana", "Afrikaans" to "Neem Dosis", "Sepedi" to "Tsea Sehlare", "Setswana" to "Tsea Molemo", "isiXhosa" to "Thatha Umyeza"),
            "Mark Taken" to mapOf("isiZulu" to "Faka Uphawu Lokuthi Uthathiwe", "Sesotho" to "Tshwaya Hore e Tserwe", "Afrikaans" to "Merk as Genoom", "Sepedi" to "Lswaya Gore e Tserwe", "Setswana" to "Tshwaya Gore e Tserwe", "isiXhosa" to "Beka Uphawu Lokuba Ithathwe"),
            "Find a clinic" to mapOf("isiZulu" to "Thola ikliniki", "Sesotho" to "Batla tleliniki", "Afrikaans" to "Vind kliniek", "Sepedi" to "Seka tleleniki", "Setswana" to "Batla tleliniki", "isiXhosa" to "Fumana ikliniki"),
            "Hospitals" to mapOf("isiZulu" to "Izibhedlela", "Sesotho" to "Dipetlele", "Afrikaans" to "Hospitale", "Sepedi" to "Dipetlele", "Setswana" to "Dipatela", "isiXhosa" to "Izibhedlele"),
            "Public Clinics" to mapOf("isiZulu" to "Amakliniki Omphakathi", "Sesotho" to "Ditleliniki tsa Sechaba", "Afrikaans" to "Publieke Klinieke", "Sepedi" to "Ditleleniki tsa Setšhaba", "Setswana" to "Ditleleniki tsa Setšhaba", "isiXhosa" to "Amakliniki Omphakathi"),
            "Private Hospitals" to mapOf("isiZulu" to "Izibhedlela Ezizimele", "Sesotho" to "Dipetlele tseo e seng tsa Mmuso", "Afrikaans" to "Private Hospitale", "Sepedi" to "Dipetlele tša Phraevete", "Setswana" to "Dipatela tsa Poraefete", "isiXhosa" to "Izibhedlele Ezizimeleyo"),
            "Pharmacies" to mapOf("isiZulu" to "Amakhemisi", "Sesotho" to "Likhemisi", "Afrikaans" to "Apteke", "Sepedi" to "Dilekhemisi", "Setswana" to "Dikhemisi", "isiXhosa" to "Iikhemesti"),
            "Emergency Care" to mapOf("isiZulu" to "Ukunakekelwa Kwezimo Eziphuthumayo", "Sesotho" to "Tlhokomelo ya Tshahlelo", "Afrikaans" to "Nooddsorg", "Sepedi" to "Tlhokomelo ya Tšhoganetšo", "Setswana" to "Tlhokomelo ya Toshoganyetso", "isiXhosa" to "Ukhathalelo Lwezimo Ezingxamisekileyo"),
            "Medication reminders" to mapOf("isiZulu" to "Izikhumbuzo zemithi", "Sesotho" to "Dikgopotso tsa meriana", "Afrikaans" to "Medikasie herinneringe", "Sepedi" to "Dikgopotso tsa dihlare", "Setswana" to "Dikgopotso tsa meriana", "isiXhosa" to "Izikhumbuzo zemiyeza"),
            "Sign Out" to mapOf("isiZulu" to "Phuma", "Sesotho" to "Tswa", "Afrikaans" to "Meld af", "Sepedi" to "Tswa", "Setswana" to "Tswaa", "isiXhosa" to "Phuma")
        )

        val langCode = when {
            languageName.equals("isiZulu", ignoreCase = true) -> "isiZulu"
            languageName.equals("Sesotho", ignoreCase = true) -> "Sesotho"
            languageName.equals("Afrikaans", ignoreCase = true) -> "Afrikaans"
            languageName.equals("Sepedi", ignoreCase = true) -> "Sepedi"
            languageName.equals("Setswana", ignoreCase = true) -> "Setswana"
            languageName.equals("isiXhosa", ignoreCase = true) -> "isiXhosa"
            else -> "English"
        }

        if (langCode == "English") return phrase

        // Check exact match first
        val exactMatch = phraseMap[trimmed]?.get(langCode)
        if (!exactMatch.isNullOrBlank()) return exactMatch

        // Partial phrase match for titles containing substrings
        for ((key, translations) in phraseMap) {
            if (trimmed.contains(key, ignoreCase = true)) {
                val trans = translations[langCode]
                if (!trans.isNullOrBlank()) {
                    return trimmed.replace(key, trans, ignoreCase = true)
                }
            }
        }

        return phrase
    }
}
