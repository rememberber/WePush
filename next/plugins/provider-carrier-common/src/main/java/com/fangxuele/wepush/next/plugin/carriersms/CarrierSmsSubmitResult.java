package com.fangxuele.wepush.next.plugin.carriersms;

import com.fangxuele.wepush.next.provider.spi.ErrorCategory;

record CarrierSmsSubmitResult(boolean success, String code, ErrorCategory category,
                              boolean retryable, String messageId, String diagnostic) { }
