package com.qs.takeout.modules.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qs.takeout.modules.pay.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
