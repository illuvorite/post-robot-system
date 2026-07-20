package com.lu.postrobotsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lu.postrobotsystem.mapper.AlertMapper;
import com.lu.postrobotsystem.model.entity.Alert;
import com.lu.postrobotsystem.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 告警服务实现
 */
@Slf4j
@Service
public class AlertServiceImpl extends ServiceImpl<AlertMapper, Alert> implements AlertService {
}
