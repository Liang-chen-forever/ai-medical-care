package com.liang.medical.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTools {

    //如果工具名字足够说明关系则不需要value属性，处理不当会引起大模型的幻觉
    //name是工具的名称，value是工具的描述
    //@P(value = "参数描述",required = true) 表示参数是否必填
    @Tool(name = "加法运算",value = "将两个参数a和b相加并返回运算结果")
    public double sum(
            @ToolMemoryId int memoryId,
            @P(value = "第一个加数",required = true) double a,
            @P(value = "第二个加数",required = true) double b){
        System.out.println("调用加法运算 memoryId: " + memoryId);
        return a+b;
    }

    @Tool(name = "平方根运算",value = "将参数x的平方根并返回运算结果")
    public double squareRoot(@ToolMemoryId int memoryId, double x){
        System.out.println("调用平方根运算"+memoryId);
        return Math.sqrt(x);
    }
}
