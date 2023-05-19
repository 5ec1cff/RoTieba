package io.github.a13e300.ro_tieba

data class Emotion(
    val name: String,
    val id: String,
    val resource: Int
)

object Emotions {
    val emotionMap = HashMap<String, Emotion>().apply {
        put("image_emoticon", Emotion("#(呵呵)", "image_emoticon", R.drawable.emoji_image_emoticon))
        put(
            "image_emoticon2",
            Emotion("#(哈哈)", "image_emoticon2", R.drawable.emoji_image_emoticon2)
        )
        put(
            "image_emoticon3",
            Emotion("#(吐舌)", "image_emoticon3", R.drawable.emoji_image_emoticon3)
        )
        put(
            "image_emoticon4",
            Emotion("#(啊)", "image_emoticon4", R.drawable.emoji_image_emoticon4)
        )
        put(
            "image_emoticon5",
            Emotion("#(酷)", "image_emoticon5", R.drawable.emoji_image_emoticon5)
        )
        put(
            "image_emoticon6",
            Emotion("#(怒)", "image_emoticon6", R.drawable.emoji_image_emoticon6)
        )
        put(
            "image_emoticon7",
            Emotion("#(开心)", "image_emoticon7", R.drawable.emoji_image_emoticon7)
        )
        put(
            "image_emoticon8",
            Emotion("#(汗)", "image_emoticon8", R.drawable.emoji_image_emoticon8)
        )
        put(
            "image_emoticon9",
            Emotion("#(泪)", "image_emoticon9", R.drawable.emoji_image_emoticon9)
        )
        put(
            "image_emoticon10",
            Emotion("#(黑线)", "image_emoticon10", R.drawable.emoji_image_emoticon10)
        )
        put(
            "image_emoticon11",
            Emotion("#(鄙视)", "image_emoticon11", R.drawable.emoji_image_emoticon11)
        )
        put(
            "image_emoticon12",
            Emotion("#(不高兴)", "image_emoticon12", R.drawable.emoji_image_emoticon12)
        )
        put(
            "image_emoticon13",
            Emotion("#(真棒)", "image_emoticon13", R.drawable.emoji_image_emoticon13)
        )
        put(
            "image_emoticon14",
            Emotion("#(钱)", "image_emoticon14", R.drawable.emoji_image_emoticon14)
        )
        put(
            "image_emoticon15",
            Emotion("#(疑问)", "image_emoticon15", R.drawable.emoji_image_emoticon15)
        )
        put(
            "image_emoticon16",
            Emotion("#(阴险)", "image_emoticon16", R.drawable.emoji_image_emoticon16)
        )
        put(
            "image_emoticon17",
            Emotion("#(吐)", "image_emoticon17", R.drawable.emoji_image_emoticon17)
        )
        put(
            "image_emoticon18",
            Emotion("#(咦)", "image_emoticon18", R.drawable.emoji_image_emoticon18)
        )
        put(
            "image_emoticon19",
            Emotion("#(委屈)", "image_emoticon19", R.drawable.emoji_image_emoticon19)
        )
        put(
            "image_emoticon20",
            Emotion("#(花心)", "image_emoticon20", R.drawable.emoji_image_emoticon20)
        )
        put(
            "image_emoticon21",
            Emotion("#(呼~)", "image_emoticon21", R.drawable.emoji_image_emoticon21)
        )
        put(
            "image_emoticon22",
            Emotion("#(笑眼)", "image_emoticon22", R.drawable.emoji_image_emoticon22)
        )
        put(
            "image_emoticon23",
            Emotion("#(冷)", "image_emoticon23", R.drawable.emoji_image_emoticon23)
        )
        put(
            "image_emoticon24",
            Emotion("#(太开心)", "image_emoticon24", R.drawable.emoji_image_emoticon24)
        )
        put(
            "image_emoticon25",
            Emotion("#(滑稽)", "image_emoticon25", R.drawable.emoji_image_emoticon25)
        )
        put(
            "image_emoticon26",
            Emotion("#(勉强)", "image_emoticon26", R.drawable.emoji_image_emoticon26)
        )
        put(
            "image_emoticon27",
            Emotion("#(狂汗)", "image_emoticon27", R.drawable.emoji_image_emoticon27)
        )
        put(
            "image_emoticon28",
            Emotion("#(乖)", "image_emoticon28", R.drawable.emoji_image_emoticon28)
        )
        put(
            "image_emoticon29",
            Emotion("#(睡觉)", "image_emoticon29", R.drawable.emoji_image_emoticon29)
        )
        put(
            "image_emoticon30",
            Emotion("#(惊哭)", "image_emoticon30", R.drawable.emoji_image_emoticon30)
        )
        put(
            "image_emoticon31",
            Emotion("#(升起)", "image_emoticon31", R.drawable.emoji_image_emoticon31)
        )
        put(
            "image_emoticon32",
            Emotion("#(惊讶)", "image_emoticon32", R.drawable.emoji_image_emoticon32)
        )
        put(
            "image_emoticon33",
            Emotion("#(喷)", "image_emoticon33", R.drawable.emoji_image_emoticon33)
        )
        put(
            "image_emoticon61",
            Emotion("#(哼)", "image_emoticon61", R.drawable.emoji_image_emoticon61)
        )
        put(
            "image_emoticon62",
            Emotion("#(吃瓜)", "image_emoticon62", R.drawable.emoji_image_emoticon62)
        )
        put(
            "image_emoticon63",
            Emotion("#(扔便便)", "image_emoticon63", R.drawable.emoji_image_emoticon63)
        )
        put(
            "image_emoticon64",
            Emotion("#(惊恐)", "image_emoticon64", R.drawable.emoji_image_emoticon64)
        )
        put(
            "image_emoticon65",
            Emotion("#(哎呦)", "image_emoticon65", R.drawable.emoji_image_emoticon65)
        )
        put(
            "image_emoticon66",
            Emotion("#(小乖)", "image_emoticon66", R.drawable.emoji_image_emoticon66)
        )
        put(
            "image_emoticon67",
            Emotion("#(捂嘴笑)", "image_emoticon67", R.drawable.emoji_image_emoticon67)
        )
        put(
            "image_emoticon68",
            Emotion("#(你懂的)", "image_emoticon68", R.drawable.emoji_image_emoticon68)
        )
        put(
            "image_emoticon69",
            Emotion("#(what)", "image_emoticon69", R.drawable.emoji_image_emoticon69)
        )
        put(
            "image_emoticon70",
            Emotion("#(酸爽)", "image_emoticon70", R.drawable.emoji_image_emoticon70)
        )
        put(
            "image_emoticon71",
            Emotion("#(呀咩爹)", "image_emoticon71", R.drawable.emoji_image_emoticon71)
        )
        put(
            "image_emoticon72",
            Emotion("#(笑尿)", "image_emoticon72", R.drawable.emoji_image_emoticon72)
        )
        put(
            "image_emoticon73",
            Emotion("#(挖鼻)", "image_emoticon73", R.drawable.emoji_image_emoticon73)
        )
        put(
            "image_emoticon74",
            Emotion("#(犀利)", "image_emoticon74", R.drawable.emoji_image_emoticon74)
        )
        put(
            "image_emoticon75",
            Emotion("#(小红脸)", "image_emoticon75", R.drawable.emoji_image_emoticon75)
        )
        put(
            "image_emoticon76",
            Emotion("#(懒得理)", "image_emoticon76", R.drawable.emoji_image_emoticon76)
        )
        put(
            "image_emoticon85",
            Emotion("#(暗中观察)", "image_emoticon85", R.drawable.emoji_image_emoticon85)
        )
        put(
            "image_emoticon86",
            Emotion("#(吃瓜)", "image_emoticon86", R.drawable.emoji_image_emoticon86)
        )
        put(
            "image_emoticon87",
            Emotion("#(喝酒)", "image_emoticon87", R.drawable.emoji_image_emoticon87)
        )
        put(
            "image_emoticon88",
            Emotion("#(嘿嘿嘿)", "image_emoticon88", R.drawable.emoji_image_emoticon88)
        )
        put(
            "image_emoticon89",
            Emotion("#(噗)", "image_emoticon89", R.drawable.emoji_image_emoticon89)
        )
        put(
            "image_emoticon90",
            Emotion("#(困成狗)", "image_emoticon90", R.drawable.emoji_image_emoticon90)
        )
        put(
            "image_emoticon91",
            Emotion("#(微微一笑)", "image_emoticon91", R.drawable.emoji_image_emoticon91)
        )
        put(
            "image_emoticon92",
            Emotion("#(托腮)", "image_emoticon92", R.drawable.emoji_image_emoticon92)
        )
        put(
            "image_emoticon93",
            Emotion("#(摊手)", "image_emoticon93", R.drawable.emoji_image_emoticon93)
        )
        put(
            "image_emoticon94",
            Emotion("#(柯基暗中观察)", "image_emoticon94", R.drawable.emoji_image_emoticon94)
        )
        put(
            "image_emoticon95",
            Emotion("#(欢呼)", "image_emoticon95", R.drawable.emoji_image_emoticon95)
        )
        put(
            "image_emoticon96",
            Emotion("#(炸药)", "image_emoticon96", R.drawable.emoji_image_emoticon96)
        )
        put(
            "image_emoticon133",
            Emotion("#(香槟)", "image_emoticon133", R.drawable.emoji_image_emoticon133)
        )
        put(
            "image_emoticon97",
            Emotion("#(突然兴奋)", "image_emoticon97", R.drawable.emoji_image_emoticon97)
        )
        put(
            "image_emoticon98",
            Emotion("#(紧张)", "image_emoticon98", R.drawable.emoji_image_emoticon98)
        )
        put(
            "image_emoticon99",
            Emotion("#(黑头瞪眼)", "image_emoticon99", R.drawable.emoji_image_emoticon99)
        )
        put(
            "image_emoticon100",
            Emotion("#(黑头高兴)", "image_emoticon100", R.drawable.emoji_image_emoticon100)
        )
        put(
            "image_emoticon125",
            Emotion("#(奥特曼)", "image_emoticon125", R.drawable.emoji_image_emoticon125)
        )
        put(
            "image_emoticon126",
            Emotion("#(不听)", "image_emoticon126", R.drawable.emoji_image_emoticon126)
        )
        put(
            "image_emoticon127",
            Emotion("#(干饭)", "image_emoticon127", R.drawable.emoji_image_emoticon127)
        )
        put(
            "image_emoticon129",
            Emotion("#(菜狗)", "image_emoticon129", R.drawable.emoji_image_emoticon129)
        )
        put(
            "image_emoticon130",
            Emotion("#(老虎)", "image_emoticon130", R.drawable.emoji_image_emoticon130)
        )
        put(
            "image_emoticon131",
            Emotion("#(嗷呜)", "image_emoticon131", R.drawable.emoji_image_emoticon131)
        )
        put(
            "image_emoticon132",
            Emotion("#(烟花)", "image_emoticon132", R.drawable.emoji_image_emoticon132)
        )
        put(
            "image_emoticon128",
            Emotion("#(望远镜)", "image_emoticon128", R.drawable.emoji_image_emoticon128)
        )
        put(
            "image_emoticon134",
            Emotion("#(文字啊)", "image_emoticon134", R.drawable.emoji_image_emoticon134)
        )
        put(
            "image_emoticon135",
            Emotion("#(文字对)", "image_emoticon135", R.drawable.emoji_image_emoticon135)
        )
        put(
            "image_emoticon136",
            Emotion("#(鼠1)", "image_emoticon136", R.drawable.emoji_image_emoticon136)
        )
        put(
            "image_emoticon137",
            Emotion("#(鼠2)", "image_emoticon137", R.drawable.emoji_image_emoticon137)
        )
        put(
            "shoubai_emoji_face_01",
            Emotion("[微笑]", "shoubai_emoji_face_01", R.drawable.emoji_shoubai_emoji_face_01)
        )
        put(
            "shoubai_emoji_face_02",
            Emotion("[开心]", "shoubai_emoji_face_02", R.drawable.emoji_shoubai_emoji_face_02)
        )
        put(
            "shoubai_emoji_face_03",
            Emotion("[期待]", "shoubai_emoji_face_03", R.drawable.emoji_shoubai_emoji_face_03)
        )
        put(
            "shoubai_emoji_face_04",
            Emotion("[大笑]", "shoubai_emoji_face_04", R.drawable.emoji_shoubai_emoji_face_04)
        )
        put(
            "shoubai_emoji_face_05",
            Emotion("[鼓掌]", "shoubai_emoji_face_05", R.drawable.emoji_shoubai_emoji_face_05)
        )
        put(
            "shoubai_emoji_face_06",
            Emotion("[悠闲]", "shoubai_emoji_face_06", R.drawable.emoji_shoubai_emoji_face_06)
        )
        put(
            "shoubai_emoji_face_07",
            Emotion("[笑哭]", "shoubai_emoji_face_07", R.drawable.emoji_shoubai_emoji_face_07)
        )
        put(
            "shoubai_emoji_face_08",
            Emotion("[不要啊]", "shoubai_emoji_face_08", R.drawable.emoji_shoubai_emoji_face_08)
        )
        put(
            "shoubai_emoji_face_09",
            Emotion("[啊]", "shoubai_emoji_face_09", R.drawable.emoji_shoubai_emoji_face_09)
        )
        put(
            "shoubai_emoji_face_10",
            Emotion("[哟]", "shoubai_emoji_face_10", R.drawable.emoji_shoubai_emoji_face_10)
        )
        put(
            "shoubai_emoji_face_11",
            Emotion("[汗]", "shoubai_emoji_face_11", R.drawable.emoji_shoubai_emoji_face_11)
        )
        put(
            "shoubai_emoji_face_12",
            Emotion("[抠鼻]", "shoubai_emoji_face_12", R.drawable.emoji_shoubai_emoji_face_12)
        )
        put(
            "shoubai_emoji_face_13",
            Emotion("[哼]", "shoubai_emoji_face_13", R.drawable.emoji_shoubai_emoji_face_13)
        )
        put(
            "shoubai_emoji_face_14",
            Emotion("[发怒]", "shoubai_emoji_face_14", R.drawable.emoji_shoubai_emoji_face_14)
        )
        put(
            "shoubai_emoji_face_15",
            Emotion("[委屈]", "shoubai_emoji_face_15", R.drawable.emoji_shoubai_emoji_face_15)
        )
        put(
            "shoubai_emoji_face_16",
            Emotion("[不高兴]", "shoubai_emoji_face_16", R.drawable.emoji_shoubai_emoji_face_16)
        )
        put(
            "shoubai_emoji_face_17",
            Emotion("[囧]", "shoubai_emoji_face_17", R.drawable.emoji_shoubai_emoji_face_17)
        )
        put(
            "shoubai_emoji_face_18",
            Emotion("[惊哭]", "shoubai_emoji_face_18", R.drawable.emoji_shoubai_emoji_face_18)
        )
        put(
            "shoubai_emoji_face_19",
            Emotion("[大哭]", "shoubai_emoji_face_19", R.drawable.emoji_shoubai_emoji_face_19)
        )
        put(
            "shoubai_emoji_face_20",
            Emotion("[流泪]", "shoubai_emoji_face_20", R.drawable.emoji_shoubai_emoji_face_20)
        )
        put(
            "shoubai_emoji_face_21",
            Emotion("[害羞]", "shoubai_emoji_face_21", R.drawable.emoji_shoubai_emoji_face_21)
        )
        put(
            "shoubai_emoji_face_22",
            Emotion("[亲亲]", "shoubai_emoji_face_22", R.drawable.emoji_shoubai_emoji_face_22)
        )
        put(
            "shoubai_emoji_face_23",
            Emotion("[色]", "shoubai_emoji_face_23", R.drawable.emoji_shoubai_emoji_face_23)
        )
        put(
            "shoubai_emoji_face_24",
            Emotion("[舔屏]", "shoubai_emoji_face_24", R.drawable.emoji_shoubai_emoji_face_24)
        )
        put(
            "shoubai_emoji_face_25",
            Emotion("[得意]", "shoubai_emoji_face_25", R.drawable.emoji_shoubai_emoji_face_25)
        )
        put(
            "shoubai_emoji_face_26",
            Emotion("[疑问]", "shoubai_emoji_face_26", R.drawable.emoji_shoubai_emoji_face_26)
        )
        put(
            "shoubai_emoji_face_27",
            Emotion("[晕]", "shoubai_emoji_face_27", R.drawable.emoji_shoubai_emoji_face_27)
        )
        put(
            "shoubai_emoji_face_28",
            Emotion("[大哈]", "shoubai_emoji_face_28", R.drawable.emoji_shoubai_emoji_face_28)
        )
        put(
            "shoubai_emoji_face_29",
            Emotion("[二哈]", "shoubai_emoji_face_29", R.drawable.emoji_shoubai_emoji_face_29)
        )
        put(
            "shoubai_emoji_face_30",
            Emotion("[三哈]", "shoubai_emoji_face_30", R.drawable.emoji_shoubai_emoji_face_30)
        )
        put(
            "shoubai_emoji_face_31",
            Emotion("[白眼]", "shoubai_emoji_face_31", R.drawable.emoji_shoubai_emoji_face_31)
        )
        put(
            "shoubai_emoji_face_32",
            Emotion("[阴险]", "shoubai_emoji_face_32", R.drawable.emoji_shoubai_emoji_face_32)
        )
        put(
            "shoubai_emoji_face_33",
            Emotion("[你懂的]", "shoubai_emoji_face_33", R.drawable.emoji_shoubai_emoji_face_33)
        )
        put(
            "shoubai_emoji_face_34",
            Emotion("[偷笑]", "shoubai_emoji_face_34", R.drawable.emoji_shoubai_emoji_face_34)
        )
        put(
            "shoubai_emoji_face_35",
            Emotion("[睡觉]", "shoubai_emoji_face_35", R.drawable.emoji_shoubai_emoji_face_35)
        )
        put(
            "shoubai_emoji_face_36",
            Emotion("[哈欠]", "shoubai_emoji_face_36", R.drawable.emoji_shoubai_emoji_face_36)
        )
        put(
            "shoubai_emoji_face_37",
            Emotion("[再见]", "shoubai_emoji_face_37", R.drawable.emoji_shoubai_emoji_face_37)
        )
        put(
            "shoubai_emoji_face_38",
            Emotion("[鄙视]", "shoubai_emoji_face_38", R.drawable.emoji_shoubai_emoji_face_38)
        )
        put(
            "shoubai_emoji_face_39",
            Emotion("[抓狂]", "shoubai_emoji_face_39", R.drawable.emoji_shoubai_emoji_face_39)
        )
        put(
            "shoubai_emoji_face_40",
            Emotion("[咒骂]", "shoubai_emoji_face_40", R.drawable.emoji_shoubai_emoji_face_40)
        )
        put(
            "shoubai_emoji_face_41",
            Emotion("[衰]", "shoubai_emoji_face_41", R.drawable.emoji_shoubai_emoji_face_41)
        )
        put(
            "shoubai_emoji_face_42",
            Emotion("[骷髅]", "shoubai_emoji_face_42", R.drawable.emoji_shoubai_emoji_face_42)
        )
        put(
            "shoubai_emoji_face_43",
            Emotion("[嘘]", "shoubai_emoji_face_43", R.drawable.emoji_shoubai_emoji_face_43)
        )
        put(
            "shoubai_emoji_face_44",
            Emotion("[闭嘴]", "shoubai_emoji_face_44", R.drawable.emoji_shoubai_emoji_face_44)
        )
        put(
            "shoubai_emoji_face_45",
            Emotion("[呆]", "shoubai_emoji_face_45", R.drawable.emoji_shoubai_emoji_face_45)
        )
        put(
            "shoubai_emoji_face_46",
            Emotion("[什么鬼]", "shoubai_emoji_face_46", R.drawable.emoji_shoubai_emoji_face_46)
        )
        put(
            "shoubai_emoji_face_47",
            Emotion("[吐]", "shoubai_emoji_face_47", R.drawable.emoji_shoubai_emoji_face_47)
        )
        put(
            "shoubai_emoji_face_48",
            Emotion("[已阅]", "shoubai_emoji_face_48", R.drawable.emoji_shoubai_emoji_face_48)
        )
        put(
            "shoubai_emoji_face_49",
            Emotion("[同上]", "shoubai_emoji_face_49", R.drawable.emoji_shoubai_emoji_face_49)
        )
        put(
            "shoubai_emoji_face_50",
            Emotion("[友军]", "shoubai_emoji_face_50", R.drawable.emoji_shoubai_emoji_face_50)
        )
        put(
            "shoubai_emoji_face_51",
            Emotion("[爱钱]", "shoubai_emoji_face_51", R.drawable.emoji_shoubai_emoji_face_51)
        )
        put(
            "shoubai_emoji_face_52",
            Emotion("[Freestyle]", "shoubai_emoji_face_52", R.drawable.emoji_shoubai_emoji_face_52)
        )
        put(
            "shoubai_emoji_face_53",
            Emotion("[国宝]", "shoubai_emoji_face_53", R.drawable.emoji_shoubai_emoji_face_53)
        )
        put(
            "shoubai_emoji_face_54",
            Emotion("[羊驼]", "shoubai_emoji_face_54", R.drawable.emoji_shoubai_emoji_face_54)
        )
        put(
            "shoubai_emoji_face_55",
            Emotion("[鲜花]", "shoubai_emoji_face_55", R.drawable.emoji_shoubai_emoji_face_55)
        )
        put(
            "shoubai_emoji_face_56",
            Emotion("[中国加油]", "shoubai_emoji_face_56", R.drawable.emoji_shoubai_emoji_face_56)
        )
        put(
            "shoubai_emoji_face_57",
            Emotion("[庆祝]", "shoubai_emoji_face_57", R.drawable.emoji_shoubai_emoji_face_57)
        )
        put(
            "shoubai_emoji_face_58",
            Emotion("[生日蛋糕]", "shoubai_emoji_face_58", R.drawable.emoji_shoubai_emoji_face_58)
        )
        put(
            "shoubai_emoji_face_59",
            Emotion("[MicDrop]", "shoubai_emoji_face_59", R.drawable.emoji_shoubai_emoji_face_59)
        )
        put(
            "shoubai_emoji_face_60",
            Emotion("[赞同]", "shoubai_emoji_face_60", R.drawable.emoji_shoubai_emoji_face_60)
        )
        put(
            "shoubai_emoji_face_61",
            Emotion("[药丸]", "shoubai_emoji_face_61", R.drawable.emoji_shoubai_emoji_face_61)
        )
        put(
            "shoubai_emoji_face_62",
            Emotion("[蜡烛]", "shoubai_emoji_face_62", R.drawable.emoji_shoubai_emoji_face_62)
        )
        put(
            "shoubai_emoji_face_63",
            Emotion("[鸡蛋]", "shoubai_emoji_face_63", R.drawable.emoji_shoubai_emoji_face_63)
        )
        put(
            "shoubai_emoji_face_64",
            Emotion("[浪]", "shoubai_emoji_face_64", R.drawable.emoji_shoubai_emoji_face_64)
        )
        put(
            "shoubai_emoji_face_65",
            Emotion("[打call]", "shoubai_emoji_face_65", R.drawable.emoji_shoubai_emoji_face_65)
        )
        put(
            "shoubai_emoji_face_66",
            Emotion("[尬笑]", "shoubai_emoji_face_66", R.drawable.emoji_shoubai_emoji_face_66)
        )
        put(
            "shoubai_emoji_face_67",
            Emotion("[坏笑]", "shoubai_emoji_face_67", R.drawable.emoji_shoubai_emoji_face_67)
        )
        put(
            "shoubai_emoji_face_68",
            Emotion("[没眼看]", "shoubai_emoji_face_68", R.drawable.emoji_shoubai_emoji_face_68)
        )
        put(
            "shoubai_emoji_face_69",
            Emotion("[嘿哈]", "shoubai_emoji_face_69", R.drawable.emoji_shoubai_emoji_face_69)
        )
        put(
            "shoubai_emoji_face_70",
            Emotion("[前面的别走]", "shoubai_emoji_face_70", R.drawable.emoji_shoubai_emoji_face_70)
        )
        put(
            "shoubai_emoji_face_71",
            Emotion("[滑稽]", "shoubai_emoji_face_71", R.drawable.emoji_shoubai_emoji_face_71)
        )
        put(
            "shoubai_emoji_face_72",
            Emotion("[捂脸]", "shoubai_emoji_face_72", R.drawable.emoji_shoubai_emoji_face_72)
        )
        put(
            "shoubai_emoji_face_73",
            Emotion("[左捂脸]", "shoubai_emoji_face_73", R.drawable.emoji_shoubai_emoji_face_73)
        )
        put(
            "shoubai_emoji_face_74",
            Emotion("[666]", "shoubai_emoji_face_74", R.drawable.emoji_shoubai_emoji_face_74)
        )
        put(
            "shoubai_emoji_face_75",
            Emotion("[2018]", "shoubai_emoji_face_75", R.drawable.emoji_shoubai_emoji_face_75)
        )
        put(
            "shoubai_emoji_face_76",
            Emotion("[福]", "shoubai_emoji_face_76", R.drawable.emoji_shoubai_emoji_face_76)
        )
        put(
            "shoubai_emoji_face_77",
            Emotion("[红包]", "shoubai_emoji_face_77", R.drawable.emoji_shoubai_emoji_face_77)
        )
        put(
            "shoubai_emoji_face_78",
            Emotion("[鞭炮]", "shoubai_emoji_face_78", R.drawable.emoji_shoubai_emoji_face_78)
        )
        put(
            "shoubai_emoji_face_79",
            Emotion("[财神]", "shoubai_emoji_face_79", R.drawable.emoji_shoubai_emoji_face_79)
        )
        put(
            "shoubai_emoji_face_80",
            Emotion("[饺子]", "shoubai_emoji_face_80", R.drawable.emoji_shoubai_emoji_face_80)
        )
        put(
            "shoubai_emoji_face_81",
            Emotion("[车票]", "shoubai_emoji_face_81", R.drawable.emoji_shoubai_emoji_face_81)
        )
        put(
            "shoubai_emoji_face_82",
            Emotion("[火车]", "shoubai_emoji_face_82", R.drawable.emoji_shoubai_emoji_face_82)
        )
        put(
            "shoubai_emoji_face_83",
            Emotion("[飞机]", "shoubai_emoji_face_83", R.drawable.emoji_shoubai_emoji_face_83)
        )
        put(
            "shoubai_emoji_face_84",
            Emotion("[射门]", "shoubai_emoji_face_84", R.drawable.emoji_shoubai_emoji_face_84)
        )
        put(
            "shoubai_emoji_face_85",
            Emotion("[红牌]", "shoubai_emoji_face_85", R.drawable.emoji_shoubai_emoji_face_85)
        )
        put(
            "shoubai_emoji_face_86",
            Emotion("[黄牌]", "shoubai_emoji_face_86", R.drawable.emoji_shoubai_emoji_face_86)
        )
        put(
            "shoubai_emoji_face_87",
            Emotion("[哨子]", "shoubai_emoji_face_87", R.drawable.emoji_shoubai_emoji_face_87)
        )
        put(
            "shoubai_emoji_face_88",
            Emotion("[比分]", "shoubai_emoji_face_88", R.drawable.emoji_shoubai_emoji_face_88)
        )
        put(
            "shoubai_emoji_face_89",
            Emotion("[啤酒]", "shoubai_emoji_face_89", R.drawable.emoji_shoubai_emoji_face_89)
        )
        put(
            "shoubai_emoji_face_90",
            Emotion("[足球]", "shoubai_emoji_face_90", R.drawable.emoji_shoubai_emoji_face_90)
        )
        put(
            "shoubai_emoji_face_91",
            Emotion("[大力神杯]", "shoubai_emoji_face_91", R.drawable.emoji_shoubai_emoji_face_91)
        )
        put(
            "shoubai_emoji_face_92",
            Emotion("[锦鲤]", "shoubai_emoji_face_92", R.drawable.emoji_shoubai_emoji_face_92)
        )
        put(
            "shoubai_emoji_face_93",
            Emotion("[2019]", "shoubai_emoji_face_93", R.drawable.emoji_shoubai_emoji_face_93)
        )
        put(
            "shoubai_emoji_face_94",
            Emotion("[猪年]", "shoubai_emoji_face_94", R.drawable.emoji_shoubai_emoji_face_94)
        )
        put(
            "shoubai_emoji_face_95",
            Emotion("[双手鼓掌]", "shoubai_emoji_face_95", R.drawable.emoji_shoubai_emoji_face_95)
        )
        put(
            "shoubai_emoji_face_96",
            Emotion("[火焰]", "shoubai_emoji_face_96", R.drawable.emoji_shoubai_emoji_face_96)
        )
        put(
            "shoubai_emoji_face_97",
            Emotion("[祈福]", "shoubai_emoji_face_97", R.drawable.emoji_shoubai_emoji_face_97)
        )
        put(
            "shoubai_emoji_face_98",
            Emotion("[亲吻]", "shoubai_emoji_face_98", R.drawable.emoji_shoubai_emoji_face_98)
        )
        put(
            "shoubai_emoji_face_99",
            Emotion("[天使]", "shoubai_emoji_face_99", R.drawable.emoji_shoubai_emoji_face_99)
        )
        put(
            "shoubai_emoji_face_100",
            Emotion("[樱花]", "shoubai_emoji_face_100", R.drawable.emoji_shoubai_emoji_face_100)
        )
        put(
            "shoubai_emoji_face_101",
            Emotion("[加油]", "shoubai_emoji_face_101", R.drawable.emoji_shoubai_emoji_face_101)
        )
        put(
            "shoubai_emoji_face_102",
            Emotion("[泡泡枪]", "shoubai_emoji_face_102", R.drawable.emoji_shoubai_emoji_face_102)
        )
        put(
            "shoubai_emoji_face_103",
            Emotion("[气球]", "shoubai_emoji_face_103", R.drawable.emoji_shoubai_emoji_face_103)
        )
        put(
            "shoubai_emoji_face_104",
            Emotion("[棒棒糖]", "shoubai_emoji_face_104", R.drawable.emoji_shoubai_emoji_face_104)
        )
        put(
            "shoubai_emoji_face_105",
            Emotion("[小黄鸭]", "shoubai_emoji_face_105", R.drawable.emoji_shoubai_emoji_face_105)
        )
        put(
            "shoubai_emoji_face_106",
            Emotion("[粽子]", "shoubai_emoji_face_106", R.drawable.emoji_shoubai_emoji_face_106)
        )
        put(
            "bearchildren_01",
            Emotion("#(熊-88)", "bearchildren_01", R.drawable.emoji_bearchildren_01)
        )
        put(
            "bearchildren_02",
            Emotion("#(熊-HI)", "bearchildren_02", R.drawable.emoji_bearchildren_02)
        )
        put(
            "bearchildren_03",
            Emotion("#(熊-人艰不拆)", "bearchildren_03", R.drawable.emoji_bearchildren_03)
        )
        put(
            "bearchildren_04",
            Emotion("#(熊-啥)", "bearchildren_04", R.drawable.emoji_bearchildren_04)
        )
        put(
            "bearchildren_05",
            Emotion("#(熊-大哭)", "bearchildren_05", R.drawable.emoji_bearchildren_05)
        )
        put(
            "bearchildren_06",
            Emotion("#(熊-失落)", "bearchildren_06", R.drawable.emoji_bearchildren_06)
        )
        put(
            "bearchildren_07",
            Emotion("#(熊-怒赞)", "bearchildren_07", R.drawable.emoji_bearchildren_07)
        )
        put(
            "bearchildren_08",
            Emotion("#(熊-惊呆了)", "bearchildren_08", R.drawable.emoji_bearchildren_08)
        )
        put(
            "bearchildren_09",
            Emotion("#(熊-李菊福)", "bearchildren_09", R.drawable.emoji_bearchildren_09)
        )
        put(
            "bearchildren_10",
            Emotion("#(熊-来信砍)", "bearchildren_10", R.drawable.emoji_bearchildren_10)
        )
        put(
            "bearchildren_11",
            Emotion("#(熊-欢迎入群)", "bearchildren_11", R.drawable.emoji_bearchildren_11)
        )
        put(
            "bearchildren_12",
            Emotion("#(熊-牛闪闪)", "bearchildren_12", R.drawable.emoji_bearchildren_12)
        )
        put(
            "bearchildren_13",
            Emotion("#(熊-生日快乐)", "bearchildren_13", R.drawable.emoji_bearchildren_13)
        )
        put(
            "bearchildren_14",
            Emotion("#(熊-石化)", "bearchildren_14", R.drawable.emoji_bearchildren_14)
        )
        put(
            "bearchildren_15",
            Emotion("#(熊-羞羞哒)", "bearchildren_15", R.drawable.emoji_bearchildren_15)
        )
        put(
            "bearchildren_16",
            Emotion("#(熊-肥皂必杀)", "bearchildren_16", R.drawable.emoji_bearchildren_16)
        )
        put(
            "bearchildren_17",
            Emotion("#(熊-谢谢你)", "bearchildren_17", R.drawable.emoji_bearchildren_17)
        )
        put(
            "bearchildren_18",
            Emotion("#(熊-跳舞)", "bearchildren_18", R.drawable.emoji_bearchildren_18)
        )
        put(
            "bearchildren_19",
            Emotion("#(熊-霹雳舞)", "bearchildren_19", R.drawable.emoji_bearchildren_19)
        )
        put(
            "bearchildren_20",
            Emotion("#(熊-鼓掌)", "bearchildren_20", R.drawable.emoji_bearchildren_20)
        )
        put(
            "image_emoticon34",
            Emotion("#(爱心)", "image_emoticon34", R.drawable.emoji_image_emoticon34)
        )
        put(
            "image_emoticon35",
            Emotion("#(心碎)", "image_emoticon35", R.drawable.emoji_image_emoticon35)
        )
        put(
            "image_emoticon36",
            Emotion("#(玫瑰)", "image_emoticon36", R.drawable.emoji_image_emoticon36)
        )
        put(
            "image_emoticon37",
            Emotion("#(礼物)", "image_emoticon37", R.drawable.emoji_image_emoticon37)
        )
        put(
            "image_emoticon38",
            Emotion("#(彩虹)", "image_emoticon38", R.drawable.emoji_image_emoticon38)
        )
        put(
            "image_emoticon39",
            Emotion("#(星星月亮)", "image_emoticon39", R.drawable.emoji_image_emoticon39)
        )
        put(
            "image_emoticon40",
            Emotion("#(太阳)", "image_emoticon40", R.drawable.emoji_image_emoticon40)
        )
        put(
            "image_emoticon41",
            Emotion("#(钱币)", "image_emoticon41", R.drawable.emoji_image_emoticon41)
        )
        put(
            "image_emoticon42",
            Emotion("#(灯泡)", "image_emoticon42", R.drawable.emoji_image_emoticon42)
        )
        put(
            "image_emoticon43",
            Emotion("#(茶杯)", "image_emoticon43", R.drawable.emoji_image_emoticon43)
        )
        put(
            "image_emoticon44",
            Emotion("#(蛋糕)", "image_emoticon44", R.drawable.emoji_image_emoticon44)
        )
        put(
            "image_emoticon45",
            Emotion("#(音乐)", "image_emoticon45", R.drawable.emoji_image_emoticon45)
        )
        put(
            "image_emoticon46",
            Emotion("#(haha)", "image_emoticon46", R.drawable.emoji_image_emoticon46)
        )
        put(
            "image_emoticon47",
            Emotion("#(胜利)", "image_emoticon47", R.drawable.emoji_image_emoticon47)
        )
        put(
            "image_emoticon48",
            Emotion("#(大拇指)", "image_emoticon48", R.drawable.emoji_image_emoticon48)
        )
        put(
            "image_emoticon49",
            Emotion("#(弱)", "image_emoticon49", R.drawable.emoji_image_emoticon49)
        )
        put(
            "image_emoticon50",
            Emotion("#(OK)", "image_emoticon50", R.drawable.emoji_image_emoticon50)
        )
        put(
            "image_emoticon77",
            Emotion("#(沙发)", "image_emoticon77", R.drawable.emoji_image_emoticon77)
        )
        put(
            "image_emoticon78",
            Emotion("#(手纸)", "image_emoticon78", R.drawable.emoji_image_emoticon78)
        )
        put(
            "image_emoticon79",
            Emotion("#(香蕉)", "image_emoticon79", R.drawable.emoji_image_emoticon79)
        )
        put(
            "image_emoticon80",
            Emotion("#(便便)", "image_emoticon80", R.drawable.emoji_image_emoticon80)
        )
        put(
            "image_emoticon81",
            Emotion("#(药丸)", "image_emoticon81", R.drawable.emoji_image_emoticon81)
        )
        put(
            "image_emoticon82",
            Emotion("#(红领巾)", "image_emoticon82", R.drawable.emoji_image_emoticon82)
        )
        put(
            "image_emoticon83",
            Emotion("#(蜡烛)", "image_emoticon83", R.drawable.emoji_image_emoticon83)
        )
        put(
            "image_emoticon84",
            Emotion("#(三道杠)", "image_emoticon84", R.drawable.emoji_image_emoticon84)
        )
        put(
            "image_emoticon101",
            Emotion("#(不跟丑人说话)", "image_emoticon101", R.drawable.emoji_image_emoticon101)
        )
        put(
            "image_emoticon102",
            Emotion("#(么么哒)", "image_emoticon102", R.drawable.emoji_image_emoticon102)
        )
        put(
            "image_emoticon103",
            Emotion("#(亲亲才能起来)", "image_emoticon103", R.drawable.emoji_image_emoticon103)
        )
        put(
            "image_emoticon104",
            Emotion("#(伦家只是宝宝)", "image_emoticon104", R.drawable.emoji_image_emoticon104)
        )
        put(
            "image_emoticon105",
            Emotion("#(你是我的人)", "image_emoticon105", R.drawable.emoji_image_emoticon105)
        )
        put(
            "image_emoticon106",
            Emotion("#(假装看不见)", "image_emoticon106", R.drawable.emoji_image_emoticon106)
        )
        put(
            "image_emoticon107",
            Emotion("#(单身等撩)", "image_emoticon107", R.drawable.emoji_image_emoticon107)
        )
        put(
            "image_emoticon108",
            Emotion("#(吓到宝宝了)", "image_emoticon108", R.drawable.emoji_image_emoticon108)
        )
        put(
            "image_emoticon109",
            Emotion("#(哈哈哈)", "image_emoticon109", R.drawable.emoji_image_emoticon109)
        )
        put(
            "image_emoticon110",
            Emotion("#(嗯嗯)", "image_emoticon110", R.drawable.emoji_image_emoticon110)
        )
        put(
            "image_emoticon111",
            Emotion("#(好幸福)", "image_emoticon111", R.drawable.emoji_image_emoticon111)
        )
        put(
            "image_emoticon112",
            Emotion("#(宝宝不开心)", "image_emoticon112", R.drawable.emoji_image_emoticon112)
        )
        put(
            "image_emoticon113",
            Emotion("#(小姐姐别走)", "image_emoticon113", R.drawable.emoji_image_emoticon113)
        )
        put(
            "image_emoticon114",
            Emotion("#(小姐姐在吗)", "image_emoticon114", R.drawable.emoji_image_emoticon114)
        )
        put(
            "image_emoticon115",
            Emotion("#(小姐姐来啦)", "image_emoticon115", R.drawable.emoji_image_emoticon115)
        )
        put(
            "image_emoticon116",
            Emotion("#(小姐姐来玩呀)", "image_emoticon116", R.drawable.emoji_image_emoticon116)
        )
        put(
            "image_emoticon117",
            Emotion("#(我养你)", "image_emoticon117", R.drawable.emoji_image_emoticon117)
        )
        put(
            "image_emoticon118",
            Emotion("#(我是不会骗你的)", "image_emoticon118", R.drawable.emoji_image_emoticon118)
        )
        put(
            "image_emoticon119",
            Emotion("#(扎心了)", "image_emoticon119", R.drawable.emoji_image_emoticon119)
        )
        put(
            "image_emoticon120",
            Emotion("#(无聊)", "image_emoticon120", R.drawable.emoji_image_emoticon120)
        )
        put(
            "image_emoticon121",
            Emotion("#(月亮代表我的心)", "image_emoticon121", R.drawable.emoji_image_emoticon121)
        )
        put(
            "image_emoticon122",
            Emotion("#(来追我呀)", "image_emoticon122", R.drawable.emoji_image_emoticon122)
        )
        put(
            "image_emoticon123",
            Emotion("#(爱你的形状)", "image_emoticon123", R.drawable.emoji_image_emoticon123)
        )
        put(
            "image_emoticon124",
            Emotion("#(白眼)", "image_emoticon124", R.drawable.emoji_image_emoticon124)
        )
    }
}
