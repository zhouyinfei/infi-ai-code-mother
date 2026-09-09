package org.infi.infiaicodemother.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.infi.infiaicodemother.model.entity.App;
import org.infi.infiaicodemother.mapper.AppMapper;
import org.infi.infiaicodemother.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/zhouyinfei">程序员infi</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
