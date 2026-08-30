package com.fangxuele.wepush.next.plugin.carriersms;

import com.fangxuele.wepush.next.core.api.CancellationToken;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.zx.sms.BaseMessage;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

final class CarrierSmsProviderSession implements ProviderSession {
    private final CarrierSmsConfig account;
    private final String contentTemplate;
    private final ProviderOpenContext context;
    private final SmsClientGateway gateway;

    private CarrierSmsProviderSession(CarrierSmsConfig account, String contentTemplate,
                                      ProviderOpenContext context, SmsClientGateway gateway) {
        this.account = account; this.contentTemplate = contentTemplate; this.context = context; this.gateway = gateway;
    }

    static CarrierSmsProviderSession dryRun(CarrierSmsConfig account, String content, ProviderOpenContext context) {
        return new CarrierSmsProviderSession(account, content, context, null);
    }

    static CarrierSmsProviderSession live(CarrierSmsConfig account, String content,
                                          ProviderOpenContext context, SmsClientGateway gateway) {
        return new CarrierSmsProviderSession(account, content, context, gateway);
    }

    @Override
    public ProviderResult send(ProviderSendRequest request, CancellationToken token) {
        if (token.cancelled()) return ProviderResult.failure("CANCELLED", ErrorCategory.CANCELLED,
                false, "Carrier SMS send was cancelled before submit");
        if (!request.deadline().isAfter(context.clock().now())) return ProviderResult.failure(
                "ITEM_TIMEOUT", ErrorCategory.TIMEOUT, false, "Carrier SMS deadline elapsed before submit");
        try {
            String phone = CarrierProviderSupport.recipient(request.recipient(), "phoneNumber");
            String content = CarrierProviderSupport.render(contentTemplate, request.recipient());
            BaseMessage message = CarrierSmsRequestFactory.create(account, phone, content);
            if (gateway == null) return ProviderResult.success("DRY_RUN", "");
            long remaining = Duration.between(context.clock().now(), request.deadline()).toMillis();
            int timeout = Math.toIntExact(Math.max(1, Math.min(remaining, account.requestTimeoutMillis())));
            List<BaseMessage> responses = gateway.submit(message, timeout);
            CarrierSmsSubmitResult parsed = CarrierSmsResponseParser.parse(account.protocol(), responses);
            if (parsed.success()) return new ProviderResult(ItemState.SUCCEEDED, parsed.code(),
                    ErrorCategory.NONE, false, null, parsed.diagnostic(), parsed.messageId(),
                    Map.of("protocol", account.protocol().name(), "fragments", Integer.toString(responses.size()),
                            "acceptance", "gateway"));
            return new ProviderResult(parsed.category() == ErrorCategory.UNKNOWN ? ItemState.UNKNOWN : ItemState.FAILED,
                    parsed.code(), parsed.category(), parsed.retryable(),
                    parsed.category() == ErrorCategory.RATE_LIMITED ? Duration.ofSeconds(1) : null,
                    parsed.diagnostic(), "", Map.of("protocol", account.protocol().name()));
        } catch (CarrierProviderProblem problem) {
            return ProviderResult.failure(problem.code(), problem.path().startsWith("recipient.")
                    ? ErrorCategory.RECIPIENT_INVALID : ErrorCategory.INVALID_REQUEST, false, problem.getMessage());
        } catch (TimeoutException problem) {
            return unknown(account.protocol() + "_SUBMIT_TIMEOUT", ErrorCategory.TIMEOUT,
                    "SMS outcome is unknown after SUBMIT_RESP timeout");
        } catch (IOException problem) {
            return unknown(account.protocol() + "_SUBMIT_IO_UNKNOWN", ErrorCategory.NETWORK,
                    "SMS outcome is unknown after gateway I/O failure");
        } catch (InterruptedException problem) {
            Thread.currentThread().interrupt();
            return unknown(account.protocol() + "_SUBMIT_INTERRUPTED", ErrorCategory.CANCELLED,
                    "SMS outcome is unknown after interruption");
        } catch (Exception problem) {
            return unknown(account.protocol() + "_SUBMIT_UNKNOWN", ErrorCategory.UNKNOWN,
                    "SMS outcome is unknown after " + problem.getClass().getSimpleName());
        }
    }

    private static ProviderResult unknown(String code, ErrorCategory category, String diagnostic) {
        return new ProviderResult(ItemState.UNKNOWN, code, category, false, null, diagnostic, "", Map.of());
    }

    @Override
    public void close() throws Exception {
        if (gateway != null) gateway.close();
    }
}
