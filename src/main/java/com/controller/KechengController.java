
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 课程
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/kecheng")
public class KechengController {
    private static final Logger logger = LoggerFactory.getLogger(KechengController.class);

    private static final String TABLE_NAME = "kecheng";

    @Autowired
    private KechengService kechengService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表非注册的service
    //注册表service
    @Autowired
    private YonghuService yonghuService;
    @Autowired
    private LaoshiService laoshiService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("学生".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("老师".equals(role))
            params.put("laoshiId",request.getSession().getAttribute("userId"));
        CommonUtil.checkMap(params);
        PageUtils page = kechengService.queryPage(params);

        //字典表数据转换
        List<KechengView> list =(List<KechengView>)page.getList();
        for(KechengView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        KechengEntity kecheng = kechengService.selectById(id);
        if(kecheng !=null){
            //entity转view
            KechengView view = new KechengView();
            BeanUtils.copyProperties( kecheng , view );//把实体数据重构到view中
            //级联表 老师
            //级联表
            LaoshiEntity laoshi = laoshiService.selectById(kecheng.getLaoshiId());
            if(laoshi != null){
            BeanUtils.copyProperties( laoshi , view ,new String[]{ "id", "createTime", "insertTime", "updateTime", "laoshiId"});//把级联的数据添加到view中,并排除id和创建时间字段,当前表的级联注册表
            view.setLaoshiId(laoshi.getId());
            }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody KechengEntity kecheng, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,kecheng:{}",this.getClass().getName(),kecheng.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");
        else if("老师".equals(role))
            kecheng.setLaoshiId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));

        Wrapper<KechengEntity> queryWrapper = new EntityWrapper<KechengEntity>()
            .eq("laoshi_id", kecheng.getLaoshiId())
            .eq("kecheng_name", kecheng.getKechengName())
            .eq("kecheng_address", kecheng.getKechengAddress())
            .eq("kecheng_types", kecheng.getKechengTypes())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        KechengEntity kechengEntity = kechengService.selectOne(queryWrapper);
        if(kechengEntity==null){
            kecheng.setInsertTime(new Date());
            kecheng.setCreateTime(new Date());
            kechengService.insert(kecheng);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody KechengEntity kecheng, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,kecheng:{}",this.getClass().getName(),kecheng.toString());
        KechengEntity oldKechengEntity = kechengService.selectById(kecheng.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
//        else if("老师".equals(role))
//            kecheng.setLaoshiId(Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId"))));
        //根据字段查询是否有相同数据
        Wrapper<KechengEntity> queryWrapper = new EntityWrapper<KechengEntity>()
            .notIn("id",kecheng.getId())
            .andNew()
            .eq("laoshi_id", kecheng.getLaoshiId())
            .eq("kecheng_name", kecheng.getKechengName())
            .eq("kecheng_address", kecheng.getKechengAddress())
            .eq("kecheng_types", kecheng.getKechengTypes())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        KechengEntity kechengEntity = kechengService.selectOne(queryWrapper);
        if("".equals(kecheng.getKechengFile()) || "null".equals(kecheng.getKechengFile())){
                kecheng.setKechengFile(null);
        }
        if(kechengEntity==null){
            kechengService.updateById(kecheng);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<KechengEntity> oldKechengList =kechengService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        kechengService.deleteBatchIds(Arrays.asList(ids));

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<KechengEntity> kechengList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            KechengEntity kechengEntity = new KechengEntity();
//                            kechengEntity.setLaoshiId(Integer.valueOf(data.get(0)));   //老师 要改的
//                            kechengEntity.setKechengUuidNumber(data.get(0));                    //课程编号 要改的
//                            kechengEntity.setKechengName(data.get(0));                    //课程名称 要改的
//                            kechengEntity.setKechengAddress(data.get(0));                    //上课地点 要改的
//                            kechengEntity.setKechengFile(data.get(0));                    //课程附件 要改的
//                            kechengEntity.setKechengTypes(Integer.valueOf(data.get(0)));   //课程类型 要改的
//                            kechengEntity.setKaishiTime(sdf.parse(data.get(0)));          //课程开始时间 要改的
//                            kechengEntity.setJieshuTime(sdf.parse(data.get(0)));          //课程结束时间 要改的
//                            kechengEntity.setJiezhiTime(sdf.parse(data.get(0)));          //截止报名时间 要改的
//                            kechengEntity.setKechengContent("");//详情和图片
//                            kechengEntity.setInsertTime(date);//时间
//                            kechengEntity.setCreateTime(date);//时间
                            kechengList.add(kechengEntity);


                            //把要查询是否重复的字段放入map中
                                //课程编号
                                if(seachFields.containsKey("kechengUuidNumber")){
                                    List<String> kechengUuidNumber = seachFields.get("kechengUuidNumber");
                                    kechengUuidNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> kechengUuidNumber = new ArrayList<>();
                                    kechengUuidNumber.add(data.get(0));//要改的
                                    seachFields.put("kechengUuidNumber",kechengUuidNumber);
                                }
                        }

                        //查询是否重复
                         //课程编号
                        List<KechengEntity> kechengEntities_kechengUuidNumber = kechengService.selectList(new EntityWrapper<KechengEntity>().in("kecheng_uuid_number", seachFields.get("kechengUuidNumber")));
                        if(kechengEntities_kechengUuidNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(KechengEntity s:kechengEntities_kechengUuidNumber){
                                repeatFields.add(s.getKechengUuidNumber());
                            }
                            return R.error(511,"数据库的该表中的 [课程编号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        kechengService.insertBatch(kechengList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }





}
