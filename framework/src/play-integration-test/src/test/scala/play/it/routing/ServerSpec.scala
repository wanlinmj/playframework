package play.it.routing

import java.util.function.Supplier

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAll
import play.api.{ BuiltInComponents, Mode }
import play.api.mvc.{ AnyContent, BodyParser }
import play.api.routing.Router
import play.core.j.JavaContextComponents
import play.it.http.{ BasicHttpClient, BasicRequest }
import play.mvc.{ Result, Results }
import play.routing.RoutingDsl
import play.server.Server
import play.{ Mode => JavaMode }

class ServerSpec extends Specification with BeforeAll {

  sequential

  override def beforeAll(): Unit = {
    System.setProperty("play.server.provider", "play.core.server.NettyServerProvider")
  }

  private def withServer[T](server: Server)(block: Server => T): T = {
    try {
      block(server)
    } finally {
      server.stop()
    }
  }

  "Java Server" should {

    "start server" in {
      "with default mode and free port" in {
        withServer(
          Server.forRouter((components) => Router.empty.asJava)
        ) { server =>
            server.httpPort() must beGreaterThan(0)
            server.underlying().mode must beEqualTo(Mode.Test)
          }
      }
      "with given port and default mode" in {
        withServer(
          Server.forRouter(9999, (components) => Router.empty.asJava)
        ) { server =>
            server.httpPort() must beEqualTo(9999)
            server.underlying().mode must beEqualTo(Mode.Test)
          }
      }
      "with the given mode and free port" in {
        withServer(
          Server.forRouter(JavaMode.DEV, (components) => Router.empty.asJava)
        ) { server =>
            server.httpPort() must beGreaterThan(0)
            server.underlying().mode must beEqualTo(Mode.Dev)
          }
      }
      "with the given mode and port" in {
        withServer(
          Server.forRouter(JavaMode.DEV, 9999, (components) => Router.empty.asJava)
        ) { server =>
            server.httpPort() must beEqualTo(9999)
            server.underlying().mode must beEqualTo(Mode.Dev)
          }
      }
      "with the given router" in {
        withServer(
          Server.forRouter(JavaMode.DEV, (components) => new RoutingDsl(components.defaultBodyParser, components.javaContextComponents)
            .GET("/something").routeTo(new Supplier[Result] {
              override def get() = Results.ok("You got something")
            }).build())
        ) { server =>
            server.underlying().mode must beEqualTo(Mode.Dev)

            val request = BasicRequest("GET", "/something", "HTTP/1.1", Map(), "")
            val responses = BasicHttpClient.makeRequests(port = server.httpPort())(request)
            responses.head.body must beEqualTo(Left("You got something"))
          }
      }
    }

    "get the address the server is running" in {
      withServer(
        Server.forRouter(9999, (components) => Router.empty.asJava)
      ) { server =>
          server.mainAddress().getPort must beEqualTo(9999)
        }
    }
  }

}
