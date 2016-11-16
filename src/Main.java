import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ZouKaifa on 20
 */
public class Main {

    /**
     * 多项式类，有描述多项式的各种属性
     */
    private class Expression {
        public String[] vars;  //变量数组
        public int length;  //项数
        public double[][] values;  //每一项中常数及各个变量的次数（列下标代表项，第一行为常数，第二行开始为变量）
        public String exp;  //标准的格式化字符串形式
    }

    private Expression exp;  //当前处理的表达式

    /**
     * 获得当前的表达式
     *
     * @return 当前表达式
     */
    public Expression getExp() {
        return exp;
    }

    /**
     * 设置正在处理的表达式
     *
     * @param exp 表达式
     */
    public void setExp(Expression exp) {
        this.exp = exp;
    }

    /**
     * 表达式判断、处理
     *
     * @param expStr 表达式字符串
     * @return 生成的表达式对象
     */
    private Expression expression(String expStr) {
        Pattern single = Pattern.compile("((\\d+)|([a-z]+(\\s*\\^\\s*[1-9]+)?))");  //字母/数字
        Pattern nape = Pattern.compile(single + "((\\s*\\*\\s*)?" + single + ")*");  //一项
        Pattern pattern = Pattern.compile("-?" + nape + "(\\s*[+-]\\s*" + nape + ")*");  //整个表达式
        Matcher matcher = pattern.matcher(expStr);
        if (matcher.find() && matcher.group().equals(expStr)) {  //匹配成功
            return deal(expStr);
        }
        return null;
    }

    /**
     * 对正确的字符串进行处理，包括含有小数、负数的字符串
     *
     * @param expStr
     * @return
     */
    private Expression deal(String expStr) {
        Expression expr = new Expression();
        Pattern tSingle = Pattern.compile("((\\d+(\\.\\d+)?)|([a-z]+(\\s*\\^\\s*[1-9]+)?))");  //可检测小数
        Pattern tNape = Pattern.compile(tSingle + "((\\s*\\*\\s*)?" + tSingle + ")*");  //含小数的项
        expr.length = expStr.split("[+-]").length;  //项数
        if (expStr.startsWith("-")) {  //不算第一项的负号
            expr.length -= 1;
        }

        TreeSet<String> varSet = new TreeSet<>();  //使用TreeSet收集变量数
        Matcher varMatch = Pattern.compile("[a-z]+").matcher(expStr);
        while (varMatch.find()) {
            varSet.add(varMatch.group());
        }
        expr.vars = new String[varSet.size()];
        expr.vars = varSet.toArray(expr.vars);  //将set转为数组
        expr.values = new double[expr.vars.length + 1][expr.length];
        for (int i = 0; i < expr.values.length; i++) {  //全赋0便于化简，常数赋1
            for (int j = 0; j < expr.values[0].length; j++) {
                expr.values[i][j] = i == 0 ? 1 : 0;
            }
        }
        expStr = expStr.replaceAll("\\s", "");  //去空格
        String temp = expStr;  //为防止重复变量或常数无法检测，每分析完一项，便利用temp删去该项
        Matcher valMatch = tNape.matcher(expStr);
        int j = 0;  //列下标
        while (valMatch.find()) {
            String val = valMatch.group();
            Matcher sin = tSingle.matcher(val);
            while (sin.find()) {  //分析单项的成分
                String re = sin.group();
                if (re.matches("\\d+(\\.\\d+)?")) {  //常数则直接相乘
                    expr.values[0][j] *= Double.parseDouble(re);
                } else {  //变量则次数相加
                    String str = re.contains("^") ? re.split("\\s*\\^\\s*")[0] : re;
                    for (int i = 0; i < expr.vars.length; i++) {  //找寻行下标
                        if (expr.vars[i].equals(str)) {
                            expr.values[i + 1][j] += re.contains("^") ? Double.parseDouble(re.split("\\s*\\^\\s*")[1]) : 1;
                            break;
                        }
                    }
                }
            }
            if (j > 0 || temp.startsWith("-")) {
                if (temp.charAt(temp.indexOf(val) - 1) == '-') {  //该项前面是否存在负号
                    expr.values[0][j] *= -1;
                }
            }
            //删除已经处理的项
            temp = temp.substring(0, temp.indexOf(val)) + temp.substring(temp.indexOf(val) + val.length());
            j++;
        }
        generate(expr);
        return expr;
    }


    /**
     * 表达式简化求值
     *
     * @param expStr 变量赋值式
     * @return 简化后的表达式对象
     */
    private Expression simplify(String expStr) {
        if ((Pattern.compile(
                "!\\s*simplify(\\s*[a-z]+\\s*=\\s*([+-]?(\\d+(\\.\\d+)?))\\s*)*")
        ).matcher(expStr).find()) {  //符合语法
            Matcher assign = Pattern.compile(
                    "[a-z]+\\s*=\\s*([+-]?(\\d+(\\.\\d+)?))").matcher(expStr);  //匹配赋值语句
            String str = exp.exp;  //格式化字符串
            /*
            使用格式化后的字符串，先将幂替换为数字，再将1次幂替换，再使用expression方法将替换
            后的字符串转为Expression对象
            */
            while (assign.find()) {
                String ass = assign.group().replaceAll("\\s", "");
                String var = ass.split("=")[0];  //变量及其值
                double value = Double.parseDouble(ass.split("=")[1]);
                Matcher varMatch = Pattern.compile(var + "\\^\\d+").matcher(str);  //匹配幂项
                while (varMatch.find()) {
                    String t = varMatch.group();
                    double newValue = Math.pow(value, Integer.parseInt(t.split("\\^")[1]));
                    //若值为负数，则仅改变这一项的符号
                    if (newValue < 0) {
                        str = changeSymbol(str, t);
                    }
                    str = str.replace(t, String.valueOf(Math.abs(newValue)));  //替换幂
                    varMatch = Pattern.compile(var + "\\^\\d+").matcher(str);  //重新匹配
                }
                //再单独替换1次变量
                if (value < 0) {
                    str = changeSymbol(str, var);
                }
                str = str.replace(var, String.valueOf(Math.abs(value)));
            }
            return deal(str);
        }
        return null;
    }


    /**
     * 幂或变量替换结果若为负数，则不使用负数替换，而改变其所在项前面的符号
     *
     * @param originStr 原多项式字串
     * @param var       会被替换为负数的幂或变量
     * @return 改变符号后的字符串（未进行变量替换，仅改变项的符号）
     */
    private String changeSymbol(String originStr, String var) {
        int index = originStr.indexOf(var);  //替换位置的索引
        StringBuffer temp = new StringBuffer(originStr);
        while (index >= 0) {  //向前寻找最近的加减号
            if (temp.charAt(index) == '+') {
                temp.setCharAt(index, '-');
                originStr = temp.toString();
                break;
            } else if (temp.charAt(index) == '-') {
                if (index == 0) {  //若负号已处于最开始，则将负号去掉
                    originStr = originStr.substring(1);
                } else {
                    temp.setCharAt(index, '+');
                    originStr = temp.toString();
                }
                break;
            }
            index--;
        }
        if (index == -1 && !originStr.startsWith("-")) {  //未找到符号，即该项为首项且正
            originStr = "-" + originStr;
        }
        return originStr;
    }

    /**
     * 表达式求导
     *
     * @param expStr 求导变量字符串
     * @return 求导后的表达式对象
     */
    private Expression derivative(String expStr) {
        Expression newExp = new Expression();
        //复制原exp的数据
        newExp.values = exp.values.clone();
        newExp.vars = exp.vars.clone();
        newExp.length = exp.length;
        newExp.exp = exp.exp;
        Pattern pat = Pattern.compile("!\\s*d/d\\s*[a-z]+");
        Matcher match = pat.matcher(expStr);  //语法检测
        if (match.find()) {
            expStr = expStr.replace("d/d", "");
            Matcher varMatch = Pattern.compile("[a-z]+").matcher(expStr);  //寻找求导的变量
            int time = 0;
            String va = "";
            while (varMatch.find()) {  //判断求导的变量个数是否为1
                va = varMatch.group();
                time++;
            }
            boolean pass = false;
            int index = 0;
            for (int i = 0; i < newExp.vars.length; i++) {
                if (newExp.vars[i].equals(va)) {  //是否存在该变量
                    pass = true;
                    index = i;
                    break;
                }
            }
            index++;
            if (pass && time == 1) {  //求导，当且仅当存在变量且只有一个
                for (int i = 0; i < newExp.length; i++) {  //每个项挨个求导
                    if (newExp.values[index][i] >= 1) {
                        newExp.values[0][i] *= newExp.values[index][i];
                        newExp.values[index][i] -= 1;
                    } else if (newExp.values[index][i] == 0) {  //若次数为0，则该项系数变0
                        newExp.values[0][i] = 0;
                    }
                }
                generate(newExp);
                return newExp;
            }
            return null;
        }
        return null;
    }

    /**
     * 根据vars及values数组，进行多项式的同类项合并，以及字符串形式再生成(直接对原多项式修改)
     */
    private void generate(Expression ex) {
        TreeMap<String, Double> napeMap = new TreeMap<>();  //用于合并同类项

        /*
         * 先生成每一项的次数字符串（如x^2*y^3*z，则字符串为"2 3 0"）
         * ，在TreeMap里以字符串为key将次数相加从而完成合并，再还原为数组形式
         */
        for (int i = 0; i < ex.length; i++) {
            String key = "";
            for (int j = 1; j < ex.values.length - 1; j++) {  //不需统计常数的次数
                key += ex.values[j][i] + " ";
            }
            if (ex.values.length > 1) {  //最后一个变量单独加入字符串，避免空格加入
                key += ex.values[ex.values.length - 1][i];
            }
            if (napeMap.containsKey(key)) {  //字串存在则相加（即合并）
                napeMap.put(key, napeMap.get(key) + ex.values[0][i]);
            } else {
                napeMap.put(key, ex.values[0][i]);
            }
            if (napeMap.get(key) == 0) {  //若该项系数为0，删掉
                napeMap.remove(key);
            }
        }


        ex.length = napeMap.size();
        ex.values = new double[ex.vars.length + 1][ex.length];  //重新还原为数组
        int i = 0;
        for (Map.Entry<String, Double> pair : napeMap.entrySet()
                ) {  //遍历TreeMap，为数组赋值
            String[] p = pair.getKey().split(" ");
            ex.values[0][i] = pair.getValue();  //常数
            if (!pair.getKey().equals("")) {  //空字串说明无变量
                for (int j = 0; j < p.length; j++) {
                    ex.values[j + 1][i] = Double.parseDouble(p[j]);
                }
            }
            ++i;
        }
        //再生成格式化的字符串
        ex.exp = "";
        for (int j = 0; j < ex.length; j++) {
            if (ex.values[0][j] < 0) {  //正负号
                ex.exp += "-";
            } else if (j > 0) {
                ex.exp += "+";
            }
            if (ex.values[0][j] == (int) (ex.values[0][j])) {  //若为整数则强转为int，避免出现x.0
                ex.exp += Math.abs((int) (ex.values[0][j]));
            } else {
                ex.exp += Math.abs(ex.values[0][j]);  //浮点数
            }
            for (int k = 0; k < ex.vars.length; k++) {  //变量处理
                if (ex.values[k + 1][j] > 0) {
                    ex.exp += "*" + ex.vars[k];
                    if (ex.values[k + 1][j] > 1) { //含有幂
                        ex.exp += "^" + (int) ex.values[k + 1][j];  //次数为整数，直接取整
                    }
                }
            }
        }
        //若是乘1则去掉
        ex.exp = ex.exp.replaceAll("^1\\*", "");  //最开始的1*
        Matcher m = Pattern.compile("[+-]1\\*").matcher(ex.exp);  //中间的+(-)1*
        if (m.find()) {
            ex.exp = ex.exp.replaceAll("[+-]1\\*", String.valueOf(m.group().charAt(0)));
        }
    }

    public static void main(String[] args) {

        Scanner scan = new Scanner(System.in);
        Main ma = new Main();
        while (true) {
            String line = scan.nextLine();  //读取输入
            if (ma.getExp() == null) {  //表达式
                Expression newExp = ma.expression(line);
                if (newExp != null) {  //正确
                    ma.setExp(newExp);
                    System.out.println(ma.getExp().exp);
                } else {  //出错
                    System.out.println("Wrong polynomial!");
                }
            } else if (line.matches("^!\\s*simplify.*")) {  //简化
                Expression sim = ma.simplify(line);
                if (sim == null) {
                    System.out.println("Wrong Assignment!");
                } else {
                    System.out.println(sim.exp);
                }
            } else if (line.matches("^!\\s*d/d.*")) {  //求导
                Expression der = ma.derivative(line);
                if (der == null) {
                    System.out.println("Error, no variable!");
                } else {
                    System.out.println(der.exp);
                }
            } else {  //其它输入
                System.out.println("Wrong Input!");
            }
        }

    }
}
