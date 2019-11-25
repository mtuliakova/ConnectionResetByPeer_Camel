import com.test.MainApplication;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import com.test.producer.ResetByPeerServer;


@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = MainApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ConnectionResetByPeerProblemIT extends CamelTestSupport {

    @Autowired
    CamelContext camelContext;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Before
    public void setUp() throws Exception {
        appEmulator(camelContext);
    }

    @Test
    public void errorWithClosedChanel() throws InterruptedException {
        ResetByPeerServer.startServer(19007);

        template.sendBody("test request");
        String resultBody = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);


        Thread.sleep(6000);

        ResetByPeerServer.stopServer();

    }

    private void appEmulator(CamelContext camelContext) throws Exception {
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .process(
                                (Exchange exchange) -> {
                                    Thread.sleep(1000);
                                })
                        .recipientList(simple("netty4-http:http://localhost:19007/testServer"))
                        .to("mock:result");
            }
        });
    }
}
