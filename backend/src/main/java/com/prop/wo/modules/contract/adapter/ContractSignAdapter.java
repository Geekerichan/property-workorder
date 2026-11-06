package com.prop.wo.modules.contract.adapter;

/**
 * 第三方电子签署适配点（二期）。
 * <p>当前电子合同为平台内「双方确认 + 时间戳」模式，未接第三方 CA；
 * 二期接入电子签章/CA 时实现本接口，并在 {@code ContractService#confirm} 中替换签署流程。</p>
 */
public interface ContractSignAdapter {

    /**
     * 向外部签署平台发起签署请求。
     *
     * @param contractNo 平台合同编号
     * @param signerName 签署人姓名
     * @param party      签署方（A=物业方，B=服务方）
     * @return 外部签署单号；平台内据此回写 {@code contract.sign_*}
     */
    String sign(String contractNo, String signerName, String party);
}
