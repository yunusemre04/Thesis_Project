package com.example.positiondeterminer.data

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit



data class PredictionApiResponse(
    val success: Boolean,
    val prediction: PredictionData,
    val api_metrics: ApiMetrics? = null,
    val model_type: String
)

data class PredictionData(
    val activity: String,
    val class_index: Int,
    val confidence: Double,
    val all_probabilities: Map<String, Double>
)

data class ApiMetrics(
    val cpu_usage_percent: Double,
    val ram_usage_mb: Double,
    val duration_seconds: Double
)



data class ModelInfoApiResponse(
    val success: Boolean,
    val model_info: ModelInfoResponse
)

data class ModelInfoResponse(
    val model_type: String? = null,
    val input_shape: List<Int>? = null,
    val output_shape: List<Int>? = null,
    val output_classes: Int? = null,
    val activities: List<String>? = null,
    val loaded: Boolean? = null,
    val num_layers: Int? = null,
    val num_parameters: Int? = null,
    val model_size_kb: Double? = null,
    val activity_labels: List<String>? = null
)

data class PyTorchModelInfoResponse(
    val success: Boolean,
    val model_format: String? = null,
    val file_name: String? = null,
    val file_size_mb: Double? = null,
    val architecture: PyTorchArchitecture? = null,
    val federated_learning: FederatedLearningStatus? = null,
    val features: List<String>? = null,
    val endpoints: Map<String, String>? = null
)

data class PyTorchArchitecture(
    val input_size: Int,
    val hidden_layers: List<Int>,
    val output_classes: Int,
    val activation: String? = null,
    val normalization: String? = null,
    val total_parameters: String? = null
)

data class FederatedLearningStatus(
    val enabled: Boolean,
    val model_loaded: Boolean,
    val current_round: Int,
    val pending_gradients: Int,
    val devices_contributing: List<String>,
    val learning_rate: Double,
    val aggregation_method: String? = null,
    val privacy_guarantees: List<String>? = null
)

data class EvaluationApiResponse(
    val success: Boolean,
    val evaluation: EvaluationResults? = null,
    val error: String? = null
)

data class EvaluationResults(
    val overall_accuracy: Double,
    val test_samples: Int,
    val precision_weighted: Double? = null,
    val recall_weighted: Double? = null,
    val f1_score_weighted: Double? = null,
    val per_class_metrics: List<PerClassMetric>? = null
)

data class PerClassMetric(
    val class_name: String,
    val precision: Double,
    val recall: Double,
    val f1_score: Double,
    val support: Int
)

// Request Models
data class PredictRequest(
    val sensor_data: List<Double>
)



data class FLGradientsRequest(
    val gradients: List<Float>,
    val activity_name: String,
    val device_id: String
)

data class GradientAggregationInfo(
    val gradient_norm: Double,
    val gradient_min: Double,
    val gradient_max: Double,
    val gradient_mean: Double,
    val privacy_preserved: Boolean,
    val raw_data_transmitted: Boolean
)

data class UpdateWeightsResponse(
    val success: Boolean,
    val message: String,
    val gradients_applied: Boolean? = null,
    val pending_aggregation: Int? = null,
    val aggregation_info: GradientAggregationInfo? = null,
    val fl_round: Int? = null,
    val auto_aggregated: Boolean? = null
)

// API Interface
interface ApiInterface {
    @POST("api/dl/predict")
    suspend fun dlPredict(@Body request: PredictRequest): Response<PredictionApiResponse>
    
    @GET("api/dl/info")
    suspend fun dlInfo(): Response<ModelInfoApiResponse>
    
    @GET("api/dl/evaluate")
    suspend fun dlEvaluate(): Response<EvaluationApiResponse>
    

    @POST("api/fl/update_weights_gradients")
    suspend fun flUpdateWeightsWithGradients(@Body request: FLGradientsRequest): Response<UpdateWeightsResponse>
    
    @GET("api/fl/info")
    suspend fun flInfo(): Response<ModelInfoApiResponse>
    
    @GET("api/fl/pytorch_model_info")
    suspend fun pytorchModelInfo(): Response<PyTorchModelInfoResponse>
    
    @GET("api/fl/evaluate_pytorch")
    suspend fun pytorchEvaluate(): Response<EvaluationApiResponse>
    
    @GET("api/fl/evaluate")
    suspend fun flEvaluate(): Response<EvaluationApiResponse>
    
    // PyTorch Model Download - uses Flask API endpoint
    @Streaming
    @GET("api/fl/get_pytorch_model")
    suspend fun downloadPyTorchModel(): Response<okhttp3.ResponseBody>
}

// API Service Singleton
object ApiService {
    private const val BASE_URL = "http://10.188.247.38:5000"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS // Only log headers, not body
    }
    
    // Standard client with minimal logging
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Download client: NO logging, LONG timeouts, retry enabled
    private val downloadClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)  // Add logging to see download requests
        .connectTimeout(60, TimeUnit.SECONDS)       // 1 minute to establish connection
        .readTimeout(300, TimeUnit.SECONDS)         // 5 minutes to read data
        .writeTimeout(60, TimeUnit.SECONDS)         // 1 minute to write
        .callTimeout(360, TimeUnit.SECONDS)         // 6 minutes total timeout
        .retryOnConnectionFailure(true)
        .build()
    
    // Standard Retrofit instance for API calls
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Download Retrofit instance - uses same Flask API with longer timeouts
    private val downloadRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)  // Changed from MODEL_SERVER_URL to BASE_URL
        .client(downloadClient)
        .build()
    
    val api: ApiInterface = retrofit.create(ApiInterface::class.java)
    
    // Download API uses same Flask server but with longer timeouts
    val downloadApi: ApiInterface = downloadRetrofit.create(ApiInterface::class.java)
    
    // Activity labels
    val ACTIVITY_LABELS = listOf(
        "WALKING",
        "WALKING_UPSTAIRS",
        "WALKING_DOWNSTAIRS",
        "SITTING",
        "STANDING",
        "LAYING"
    )
}
