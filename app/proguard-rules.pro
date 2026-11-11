# Room e Compose já trazem suas regras de consumidor.
# Nunca registrar dados sensíveis em logs de release:
-assumenosideeffects class android.util.Log { public static *** d(...); public static *** v(...); }
