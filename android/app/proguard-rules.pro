# kotlinx.serialization: mantém os serializers gerados das classes de dados
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager (vem com o SDK de anúncios) cria o banco Room por reflexão
-keep class * extends androidx.room.RoomDatabase { <init>(); }
