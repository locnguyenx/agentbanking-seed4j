package com.agentbanking.duitnow.infrastructure.iso;

public class Iso20022MessageFactory {

  public String createTransferRequest(String fromAccount, String toAccount, 
      String amount, String reference) {
    return """
      <?xml version="1.0" encoding="UTF-8"?>
      <Document xmlns="urn:iso:std:iso:20022:fin:remt.001.001">
        <CdtTrfTxInf>
          <PmtId>
            <EndToEndId>%s</EndToEndId>
          </PmtId>
          <Amt>
            <InstdAmt Ccy="MYR">%s</InstdAmt>
          </Amt>
          <CdtrAcct>
            <Id>
              <IBAN>%s</IBAN>
            </Id>
          </CdtrAcct>
          <RmtInf>
            <Ustrd>%s</Ustrd>
          </RmtInf>
        </CdtTrfTxInf>
      </Document>
      """.formatted(reference, amount, toAccount, reference);
  }

  public String createTransferResponse(String status, String reason) {
    return """
      <?xml version="1.0" encoding="UTF-8"?>
      <Document xmlns="urn:iso:std:iso:20022:fin:remt.001.001">
        <GrpHdr>
          <CtrlSum>%s</CtrlSum>
        </GrpHdr>
      </Document>
      """.formatted(status);
  }
}