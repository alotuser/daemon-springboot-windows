package cn.daemon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import cn.daemon.util.ResourcesUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;

@Mojo(name = "make-win-service", defaultPhase = LifecyclePhase.PACKAGE)
public class WindowsServiceMojo extends AbstractMojo {

	@Parameter(property="project.version",required = true)
    private String version;
	
	@Parameter(property="project.groupId",required = true,defaultValue = "")
    private String groupId;

	@Parameter(property="project.artifactId",required = true,defaultValue = "")
    private String artifactId;

    @Parameter(property="project.description")
    private String description;
    
    @Parameter(property="project.packaging")
    private String packaging;
    
	@Parameter(property="project.basedir",required = true,readonly = true)
    private File baseDir;
	
	@Parameter(property="project.build.directory",required = true)
    private File targetDir;
	
	@Parameter(property="project.build.sourceDirectory",required = true,readonly = true)
    private File sourceDir;
	
	@Parameter(property="project.build.testSourceDirectory",required = true,readonly = true)
    private File testSourceDir;

    @Parameter(property="arguments")
    private String[] arguments;

    @Parameter(property="vmOptions")
    private String vmOptions;

    @Parameter(property="programArguments")
    private String programArguments;

    @Parameter(property="projectName")
    private String projectName;
    
    @Parameter(property="isVersion",defaultValue = "true")
    private boolean isVersion;//是否需要版本号
    
    public void execute() {
        getLog().info("开始生成 Windows Service 必要的文件");
        try {
            String jarName= getJarName(isVersion),jarNames=getJarName(isVersion);
            if(!FileUtil.exist(targetDir.getPath() + File.separator +jarNames)) {
            	jarNames= getJarName();
            }
            if(!FileUtil.exist(targetDir.getPath() + File.separator +jarNames)) {
            	jarNames= getJarName(!isVersion);
            }
            if(FileUtil.exist(targetDir.getPath() + File.separator +jarNames)) {
            	/*创建文件夹*/
                File distDir = new File(targetDir, File.separator + "dist");
                if (distDir.exists()) {
                    try {
                    	FileUtil.del(distDir);
                    } catch (Exception e) {
                        getLog().error("删除目录失败！请检查文件是否在使用");
                        e.printStackTrace();
                    }
                }
                FileUtil.mkdir(distDir.getPath());
                File logDir = new File(distDir,File.separator + "logs");
                FileUtil.mkdir(logDir.getPath());
            	String jarPrefixName=getJarPrefixName(isVersion);
                /*复制文件*/
                ClassPathResource cpr_exe_file = new ClassPathResource("service.exe.yml");
                FileUtil.writeFromStream(cpr_exe_file.getStream(), new File(distDir,File.separator+jarPrefixName+".exe"));
                FileUtil.writeString(ResourcesUtil.README_FILE, new File(distDir, File.separator + "readme.txt"), CharsetUtil.UTF_8);
                FileUtil.writeString(ResourcesUtil.XML_FILE, new File(distDir,File.separator+jarPrefixName+".xml"), CharsetUtil.UTF_8);
                FileUtil.writeString(ResourcesUtil.CONFIG_FILE, new File(distDir,File.separator+jarPrefixName+".exe.config"), CharsetUtil.UTF_8);
                FileUtil.copy(new File(targetDir.getPath() + File.separator + jarNames), new File(distDir, File.separator + jarName), true);

                convert(jarName,jarPrefixName);
                
                createBat(distDir, "install.bat", "install");
                createBat(distDir, "uninstall.bat", "uninstall");
                createBat(distDir, "start.bat", "start");
                createBat(distDir, "stop.bat", "stop");
                createBat(distDir, "restart.bat", "restart");

                getLog().info("正在制作压缩包....");
                String zipDir = targetDir.getPath() + File.separator + jarPrefixName + ".zip";
                ZipUtil.zip(distDir.getPath(), zipDir);
                getLog().info("正在清除临时文件....");
                FileUtil.del(distDir);
                getLog().info("制作完成，文件:" + zipDir);
            }else {
            	 getLog().info("制作Windows Service 失败:未找到文件");
            }
            
        } catch (Exception e) {
            getLog().error("制作Windows Service 失败：",e);
        }
    }


    /**
     * 属性转化
     * @param xmlFile xml文件
     */
    private void convert(String jarName,String jarPrefixName){
        SAXReader reader = new SAXReader();
        try {
        	File xmlFile=new File(targetDir, File.separator + "dist"+File.separator+jarPrefixName+".xml");
            Document document = reader.read(xmlFile);
            Element root = document.getRootElement();
            root.element("id").setText(artifactId);
            root.element("name").setText(jarPrefixName);
            root.element("description").setText(null == description ? "暂无描述" : description);
            if (arguments.length > 0) {
                getLog().warn("arguments 参数设置已过期,参数配置可能不会生效,请分别设置 vmOptions 参数 和 programArguments 参数");
            }
            String vm_options = StrUtil.isEmpty(vmOptions) ? " " : " " + vmOptions + " ";
            String program_arguments = StrUtil.isEmpty(programArguments) ? "" : " " + programArguments;
            root.element("arguments").setText(vm_options + "-jar " + jarName +  program_arguments);
            saveXML(document,xmlFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存 XML 文件
     * @param document 文档
     * @param xmlFile xml文件
     */
    private void saveXML(Document document, File xmlFile){
        try {
            XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(xmlFile), StandardCharsets.UTF_8));
            writer.write(document);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param outDri   输出目录
     * @param fileName 文件名
     * @param text     命令文本
     */
    private void createBat(File outDri, String fileName, String text) {
        if (!outDri.exists()) {
            FileUtil.mkdir(outDri.getPath());
        }
        File file = new File(outDri, fileName);
        try (FileWriter w = new FileWriter(file)) {
            w.write("@echo off\n" +
                    "%1 mshta vbscript:CreateObject(\"Shell.Application\").ShellExecute(\"cmd.exe\",\"/c %~s0 ::\",\"\",\"runas\",1)(window.close)&&exit\n" +
                    "%~dp0" + getJarPrefixName(isVersion) + ".exe " + text + "\n" +
                    "echo The " + getJarPrefixName(isVersion) + " service current state:\n" +
                    "%~dp0" + getJarPrefixName(isVersion) + ".exe status\n" +
                    "pause");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取jar包前缀名
     * @return String
     */
    private String getJarPrefixName() {
        return getJarPrefixName(true);
    }
    private String getJarPrefixName(boolean isVersion) {
        return artifactId + (isVersion?"-"+version:"");
    }
    
    
    /**
     * 获取jar包全名
     * @return String
     */
    private String getJarName() {
        return getJarPrefixName()+"."+packaging;
    }
    private String getJarName(boolean isVersion) {
        return getJarPrefixName(isVersion)+"."+packaging;
    }
    
}
