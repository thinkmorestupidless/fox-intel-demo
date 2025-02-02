package modules

import com.google.inject.name.Named
import com.google.inject.{ AbstractModule, Provides }
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{ Environment, EventBus, Silhouette, SilhouetteProvider }
import com.mohiva.play.silhouette.crypto.{ JcaCrypter, JcaCrypterSettings, JcaSigner, JcaSignerSettings }
import com.mohiva.play.silhouette.impl.authenticators.{ JWTAuthenticator, JWTAuthenticatorService, JWTAuthenticatorSettings }
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth1._
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{ CookieSecretProvider, CookieSecretSettings }
import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import com.mohiva.play.silhouette.impl.providers.oauth2._
import com.mohiva.play.silhouette.impl.services._
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.{ DelegableAuthInfoDAO, InMemoryAuthInfoDAO }
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import models.daos._
import models.services.{ UserService, UserServiceImpl }
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.EnumerationReader._
import net.ceedubs.ficus.readers.ValueReader
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.Cookie
import utils.auth.DefaultEnv

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * The Guice module which wires all Silhouette dependencies.
 */
class SilhouetteModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  override def configure() {
    bind[Silhouette[DefaultEnv]].to[SilhouetteProvider[DefaultEnv]]
    bind[UserService].to[UserServiceImpl]
    bind[UserDAO].to[LagomUserDAO]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
    bind[PasswordHasher].toInstance(new BCryptPasswordHasher())
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())

    bind[SlickPasswordInfoDAO]

    // Replace this with the bindings to your concrete DAOs
    bind[DelegableAuthInfoDAO[PasswordInfo]].to[SlickPasswordInfoDAO]
    bind[DelegableAuthInfoDAO[OAuth1Info]].toInstance(new InMemoryAuthInfoDAO[OAuth1Info])
    bind[DelegableAuthInfoDAO[OAuth2Info]].toInstance(new InMemoryAuthInfoDAO[OAuth2Info])
    bind[DelegableAuthInfoDAO[OpenIDInfo]].toInstance(new InMemoryAuthInfoDAO[OpenIDInfo])
  }

  /**
   * Provides the HTTP layer implementation.
   *
   * @param client Play's WS client.
   * @return The HTTP layer implementation.
   */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /**
   * Provides the Silhouette environment.
   *
   * @param userService The user service implementation.
   * @param authenticatorService The authentication service implementation.
   * @param eventBus The event bus instance.
   * @return The Silhouette environment.
   */
  @Provides
  def provideEnvironment(
      userService: UserService,
      authenticatorService: AuthenticatorService[JWTAuthenticator],
      eventBus: EventBus
  ): Environment[DefaultEnv] =
    Environment[DefaultEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )

  /**
   * Provides the social provider registry.
   *
   * @param facebookProvider The Facebook provider implementation.
   * @param googleProvider The Google provider implementation.
   * @param vkProvider The VK provider implementation.
   * @param twitterProvider The Twitter provider implementation.
   * @param xingProvider The Xing provider implementation.
   * @return The Silhouette environment.
   */
  @Provides
  def provideSocialProviderRegistry(
      facebookProvider: FacebookProvider,
      googleProvider: GoogleProvider,
      vkProvider: VKProvider,
      twitterProvider: TwitterProvider,
      xingProvider: XingProvider
  ): SocialProviderRegistry =
    SocialProviderRegistry(
      Seq(
        googleProvider,
        facebookProvider,
        twitterProvider,
        vkProvider,
        xingProvider
      )
    )

  /**
   * Provides the signer for the OAuth1 token secret provider.
   *
   * @param configuration The Play configuration.
   * @return The signer for the OAuth1 token secret provider.
   */
  @Provides @Named("oauth1-token-secret-signer")
  def provideOAuth1TokenSecretSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings]("silhouette.oauth1TokenSecretProvider.signer")

    new JcaSigner(config)
  }

  /**
   * Provides the crypter for the OAuth1 token secret provider.
   *
   * @param configuration The Play configuration.
   * @return The crypter for the OAuth1 token secret provider.
   */
  @Provides @Named("oauth1-token-secret-crypter")
  def provideOAuth1TokenSecretCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.oauth1TokenSecretProvider.crypter")

    new JcaCrypter(config)
  }

  /**
   * Provides the signer for the social state handler.
   *
   * @param configuration The Play configuration.
   * @return The signer for the social state handler.
   */
  @Provides @Named("social-state-signer")
  def provideSocialStateSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings]("silhouette.socialStateHandler.signer")

    new JcaSigner(config)
  }

  /**
   * Provides the signer for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The signer for the authenticator.
   */
  @Provides
  def provideAuthenticatorSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings]("silhouette.authenticator.signer")

    new JcaSigner(config)
  }

  /**
   * Provides the crypter for the authenticator.
   *
   * @param configuration The Play configuration.
   * @return The crypter for the authenticator.
   */
  @Provides
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")

    new JcaCrypter(config)
  }

  /**
   * Provides the auth info repository.
   *
   * @param passwordInfoDAO The implementation of the delegable password auth info DAO.
   * @param oauth1InfoDAO The implementation of the delegable OAuth1 auth info DAO.
   * @param oauth2InfoDAO The implementation of the delegable OAuth2 auth info DAO.
   * @param openIDInfoDAO The implementation of the delegable OpenID auth info DAO.
   * @return The auth info repository instance.
   */
  @Provides
  def provideAuthInfoRepository(
      passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo],
      oauth1InfoDAO: DelegableAuthInfoDAO[OAuth1Info],
      oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info],
      openIDInfoDAO: DelegableAuthInfoDAO[OpenIDInfo]
  ): AuthInfoRepository =
    new DelegableAuthInfoRepository(passwordInfoDAO, oauth1InfoDAO, oauth2InfoDAO, openIDInfoDAO)

  /**
   * Provides the authenticator service.
   *
   * @param crypter The crypter implementation.
   * @param idGenerator The ID generator implementation.
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The authenticator service.
   */
  @Provides
  def provideAuthenticatorService(
      crypter: Crypter,
      idGenerator: IDGenerator,
      configuration: Configuration,
      clock: Clock
  ): AuthenticatorService[JWTAuthenticator] = {

    val config = configuration.underlying.as[JWTAuthenticatorSettings]("silhouette.authenticator")
    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new JWTAuthenticatorService(config, None, encoder, idGenerator, clock)
  }

  /**
   * Provides the avatar service.
   *
   * @param httpLayer The HTTP layer implementation.
   * @return The avatar service implementation.
   */
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = new GravatarService(httpLayer)
  ValueReader
  /**
   * Provides the OAuth1 token secret provider.
   *
   * @param signer The signer implementation.
   * @param crypter The crypter implementation.
   * @param configuration The Play configuration.
   * @param clock The clock instance.
   * @return The OAuth1 token secret provider implementation.
   */
  @Provides
  def provideOAuth1TokenSecretProvider(
      @Named("oauth1-token-secret-signer") signer: Signer,
      @Named("oauth1-token-secret-crypter") crypter: Crypter,
      configuration: Configuration,
      clock: Clock
  ): OAuth1TokenSecretProvider = {
    implicit val sameSiteReader: ValueReader[Option[Cookie.SameSite]] =
      ValueReader.relative { cfg ⇒
        if (cfg.getIsNull(".")) {
          None
        } else {
          Some(
            Cookie.SameSite.parse(cfg.as[String]).getOrElse(throw new RuntimeException("Invalid SameSite value"))
          )
        }
      }
    val settings = configuration.underlying.as[CookieSecretSettings]("silhouette.oauth1TokenSecretProvider")
    new CookieSecretProvider(settings, signer, crypter, clock)
  }

  /**
   * Provides the social state handler.
   * @return The social state handler implementation.
   */
  @Provides
  def provideSocialStateHandler(
      @Named("social-state-signer") signer: Signer
  ): SocialStateHandler =
    new DefaultSocialStateHandler(Set(), signer)

  /**
   * Provides the password hasher registry.
   *
   * @param passwordHasher The default password hasher implementation.
   * @return The password hasher registry.
   */
  @Provides
  def providePasswordHasherRegistry(passwordHasher: PasswordHasher): PasswordHasherRegistry =
    PasswordHasherRegistry(passwordHasher)

  /**
   * Provides the credentials provider.
   *
   * @param authInfoRepository The auth info repository implementation.
   * @param passwordHasherRegistry The password hasher registry.
   * @return The credentials provider.
   */
  @Provides
  def provideCredentialsProvider(
      authInfoRepository: AuthInfoRepository,
      passwordHasherRegistry: PasswordHasherRegistry
  ): CredentialsProvider =
    new CredentialsProvider(authInfoRepository, passwordHasherRegistry)

  /**
   * Provides the Facebook provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param socialStateHandler The social state handler implementation.
   * @param configuration The Play configuration.
   * @return The Facebook provider.
   */
  @Provides
  def provideFacebookProvider(
      httpLayer: HTTPLayer,
      socialStateHandler: SocialStateHandler,
      configuration: Configuration
  ): FacebookProvider =
    new FacebookProvider(
      httpLayer,
      socialStateHandler,
      configuration.underlying.as[OAuth2Settings]("silhouette.facebook"))

  /**
   * Provides the Google provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param socialStateHandler The social state handler implementation.
   * @param configuration The Play configuration.
   * @return The Google provider.
   */
  @Provides
  def provideGoogleProvider(
      httpLayer: HTTPLayer,
      socialStateHandler: SocialStateHandler,
      configuration: Configuration
  ): GoogleProvider =
    new GoogleProvider(httpLayer, socialStateHandler, configuration.underlying.as[OAuth2Settings]("silhouette.google"))

  /**
   * Provides the VK provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param socialStateHandler The social state handler implementation.
   * @param configuration The Play configuration.
   * @return The VK provider.
   */
  @Provides
  def provideVKProvider(
      httpLayer: HTTPLayer,
      socialStateHandler: SocialStateHandler,
      configuration: Configuration
  ): VKProvider =
    new VKProvider(httpLayer, socialStateHandler, configuration.underlying.as[OAuth2Settings]("silhouette.vk"))

  /**
   * Provides the Twitter provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param tokenSecretProvider The token secret provider implementation.
   * @param configuration The Play configuration.
   * @return The Twitter provider.
   */
  @Provides
  def provideTwitterProvider(
      httpLayer: HTTPLayer,
      tokenSecretProvider: OAuth1TokenSecretProvider,
      configuration: Configuration
  ): TwitterProvider = {

    val settings = configuration.underlying.as[OAuth1Settings]("silhouette.twitter")
    new TwitterProvider(httpLayer, new PlayOAuth1Service(settings), tokenSecretProvider, settings)
  }

  /**
   * Provides the Xing provider.
   *
   * @param httpLayer The HTTP layer implementation.
   * @param tokenSecretProvider The token secret provider implementation.
   * @param configuration The Play configuration.
   * @return The Xing provider.
   */
  @Provides
  def provideXingProvider(
      httpLayer: HTTPLayer,
      tokenSecretProvider: OAuth1TokenSecretProvider,
      configuration: Configuration
  ): XingProvider = {

    val settings = configuration.underlying.as[OAuth1Settings]("silhouette.xing")
    new XingProvider(httpLayer, new PlayOAuth1Service(settings), tokenSecretProvider, settings)
  }
}
