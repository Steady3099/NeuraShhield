package com.attentionmanager

import android.content.Context
import com.attentionmanager.data.database.DatabaseModule
import com.attentionmanager.data.preferences.filterPreferencesDataStore
import com.attentionmanager.data.repository.ContactPriorityRepositoryImpl
import com.attentionmanager.data.repository.DigestRepositoryImpl
import com.attentionmanager.data.repository.NotificationRepositoryImpl
import com.attentionmanager.data.repository.PreferenceRepositoryImpl
import com.attentionmanager.data.repository.SpamLogRepositoryImpl
import com.attentionmanager.domain.repository.ContactPriorityRepository
import com.attentionmanager.domain.repository.DigestRepository
import com.attentionmanager.domain.repository.NotificationRepository
import com.attentionmanager.domain.repository.PreferenceRepository
import com.attentionmanager.domain.repository.SpamLogRepository
import com.attentionmanager.domain.usecase.AppCategoryResolver
import com.attentionmanager.domain.usecase.AttentionManagerController
import com.attentionmanager.domain.usecase.DigestGeneratorUseCase
import com.attentionmanager.domain.usecase.DigestScheduler
import com.attentionmanager.domain.usecase.NoOpLocalModelTrainer
import com.attentionmanager.domain.usecase.NotificationClassifierUseCase
import com.attentionmanager.domain.usecase.UserFeedbackUseCase
import com.attentionmanager.ml.NotificationClassifier
import com.attentionmanager.service.ContextSignalProvider
import com.attentionmanager.service.DigestNotificationPublisher

class AppGraph private constructor(private val context: Context) {
    private val database by lazy { DatabaseModule.create(context) }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepositoryImpl(database.notificationDao())
    }
    val contactPriorityRepository: ContactPriorityRepository by lazy {
        ContactPriorityRepositoryImpl(database.contactPriorityDao())
    }
    val digestRepository: DigestRepository by lazy {
        DigestRepositoryImpl(database.digestDao())
    }
    val spamLogRepository: SpamLogRepository by lazy {
        SpamLogRepositoryImpl(database.spamLogDao())
    }
    val preferenceRepository: PreferenceRepository by lazy {
        PreferenceRepositoryImpl(context.filterPreferencesDataStore)
    }
    val contextSignalProvider: ContextSignalProvider by lazy { ContextSignalProvider(context) }
    val classifier: NotificationClassifier by lazy { NotificationClassifier(context) }
    val attentionController: AttentionManagerController by lazy {
        AttentionManagerController(preferenceRepository)
    }
    val notificationClassifierUseCase: NotificationClassifierUseCase by lazy {
        NotificationClassifierUseCase(
            notificationRepository = notificationRepository,
            contactPriorityRepository = contactPriorityRepository,
            spamLogRepository = spamLogRepository,
            classifier = classifier,
            contextSignalProvider = contextSignalProvider
        )
    }
    val digestGeneratorUseCase: DigestGeneratorUseCase by lazy {
        DigestGeneratorUseCase(
            notificationRepository = notificationRepository,
            digestRepository = digestRepository,
            categoryResolver = AppCategoryResolver(context),
            publisher = DigestNotificationPublisher(context)
        )
    }
    val digestScheduler: DigestScheduler by lazy { DigestScheduler(context) }
    val userFeedbackUseCase: UserFeedbackUseCase by lazy {
        UserFeedbackUseCase(
            notificationRepository = notificationRepository,
            contactPriorityRepository = contactPriorityRepository,
            spamLogRepository = spamLogRepository,
            modelTrainer = NoOpLocalModelTrainer()
        )
    }

    companion object {
        @Volatile private var instance: AppGraph? = null

        fun from(context: Context): AppGraph =
            instance ?: synchronized(this) {
                instance ?: AppGraph(context.applicationContext).also { instance = it }
            }
    }
}
