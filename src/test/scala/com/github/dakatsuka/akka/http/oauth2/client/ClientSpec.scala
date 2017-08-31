package com.github.dakatsuka.akka.http.oauth2.client

import java.net.URI

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.stream.scaladsl.Flow
import akka.stream.{ ActorMaterializer, Materializer }
import com.github.dakatsuka.akka.http.oauth2.client.Error.UnauthorizedException
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.{ BeforeAndAfterAll, DiagrammedAssertions, FlatSpec }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext }

class ClientSpec extends FlatSpec with DiagrammedAssertions with MockFactory with ScalaFutures with BeforeAndAfterAll {
  implicit val system: ActorSystem        = ActorSystem()
  implicit val ec: ExecutionContext       = system.dispatcher
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(700, Millis))

  override def afterAll(): Unit = {
    Await.ready(system.terminate(), Duration.Inf)
  }

  behavior of "Client"

  "#getAuthorizeUrl" should "delegate processing to strategy" in {
    import strategy._

    val config = mock[ConfigLike]
    (config.clientId _).expects().returning("xxx")
    (config.authorizeUrl _).expects().returning("/oauth/authorize")
    (config.site _).expects().returning(URI.create("https://example.com"))

    val client = new Client(config)
    val result = client.getAuthorizeUrl(GrantType.AuthorizationCode, Map("redirect_uri" -> "https://example.com/callback"))
    val actual = result.get.toString
    val expect = "https://example.com/oauth/authorize?redirect_uri=https://example.com/callback&response_type=code&client_id=xxx"
    assert(actual == expect)
  }

  "#getAccessToken" should "return Right[AccessToken] when oauth provider approves" in {
    import strategy._

    val response = HttpResponse(
      status = StatusCodes.OK,
      headers = Nil,
      entity = HttpEntity(
        `application/json`,
        s"""
           |{
           |  "access_token": "xxx",
           |  "token_type": "bearer",
           |  "expires_in": 86400,
           |  "refresh_token": "yyy"
           |}
         """.stripMargin
      )
    )

    val config = mock[ConfigLike]
    (config.clientId _).expects().returning("xxx")
    (config.clientSecret _).expects().returning("yyy")
    (config.site _).expects().returning(URI.create("https://example.com"))
    (config.tokenUrl _).expects().returning("/oauth/token")
    (config.tokenMethod _).expects().returning(HttpMethods.POST)
    (config
      .connection(_: ActorSystem))
      .expects(*)
      .returning(Flow[HttpRequest].map { _ =>
        response
      })

    val client = new Client(config)
    val result = client.getAccessToken(GrantType.AuthorizationCode, Map("code" -> "zzz", "redirect_uri" -> "https://example.com"))

    whenReady(result) { r =>
      assert(r.isRight)
    }
  }

  it should "return Left[UnauthorizedException] when oauth provider rejects" in {
    import strategy._

    val response = HttpResponse(
      status = StatusCodes.Unauthorized,
      headers = Nil,
      entity = HttpEntity(
        `application/json`,
        s"""
           |{
           |  "error": "invalid_client",
           |  "error_description": "description"
           |}
         """.stripMargin
      )
    )

    val config = mock[ConfigLike]
    (config.clientId _).expects().returning("xxx")
    (config.clientSecret _).expects().returning("yyy")
    (config.site _).expects().returning(URI.create("https://example.com"))
    (config.tokenUrl _).expects().returning("/oauth/token")
    (config.tokenMethod _).expects().returning(HttpMethods.POST)
    (config
      .connection(_: ActorSystem))
      .expects(*)
      .returning(Flow[HttpRequest].map { _ =>
        response
      })

    val client = new Client(config)
    val result = client.getAccessToken(GrantType.AuthorizationCode, Map("code" -> "zzz", "redirect_uri" -> "https://example.com"))

    whenReady(result) { r =>
      assert(r.isLeft)
      assert(r.left.exists(_.isInstanceOf[UnauthorizedException]))
    }
  }
}