package com.sow.wegui.client.screen;

import com.sow.wegui.client.CommandHistory;
import com.sow.wegui.client.CommandSender;
import com.sow.wegui.commands.WeCommands;
import com.sow.wegui.commands.WeCommands.Command;
import com.sow.wegui.commands.WeCommands.Usage;
import com.sow.wegui.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏命令轮盘菜单：以鼠标位置为圆心显示环形扇区，点击执行对应命令。
 * 最多 6 等分，超过分页，滚轮切换页数。
 * 渲染采用多边形扫描线填充，左右对称无像素化。
 */
public class RadialMenuScreen extends GuiBase {
    private static final int MAX_PER_PAGE = 6;

    // 现代配色：半透明深色 + 柔和高亮
    private static final int COLOR_SEGMENT_A = 0x66000000;
    private static final int COLOR_SEGMENT_B = 0x661a1a2e;
    private static final int COLOR_HOVER = 0xCC4466AA;
    private static final int COLOR_HOVER_EDGE = 0xFF88BBFF;
    private static final int COLOR_INNER = 0xE0101020;
    private static final int COLOR_INNER_RING = 0x88AACCFF;
    private static final int COLOR_OUTER_RING = 0x66AACCFF;
    private static final int COLOR_DIVIDER = 0x558899AA;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFFBBBBCC;
    private static final int COLOR_PAGE_TEXT = 0xFFDDEEFF;
    private static final int COLOR_PAGE_BG = 0x99000000;
    private static final int COLOR_PAGE_EDGE = 0x558899AA;

    private final List<Command> commands = new ArrayList<>();
    private int centerX;
    private int centerY;
    private int outerRadius;
    private int innerRadius;
    private int hoveredSegment = -1;
    private int currentPage = 0;
    private int totalPages = 1;
    private String cachedTitle;
    private String cachedEmptyHint;
    private long lastScrollTime = 0;
    private static final long SCROLL_THROTTLE_MS = 150;

    // 渲染缓冲：预计算的顶点和交点数组，避免每帧分配
    private static final int ARC_STEPS = 24;
    private static final int RING_STEPS = 48;
    private final double[] sectorVertX = new double[(ARC_STEPS + 1) * 2];
    private final double[] sectorVertY = new double[(ARC_STEPS + 1) * 2];
    private final double[] outerRingVertX = new double[RING_STEPS * 2];
    private final double[] outerRingVertY = new double[RING_STEPS * 2];
    private final double[] innerRingVertX = new double[RING_STEPS * 2];
    private final double[] innerRingVertY = new double[RING_STEPS * 2];
    private final double[] innerFilledVertX = new double[RING_STEPS];
    private final double[] innerFilledVertY = new double[RING_STEPS];
    private final double[] lineVertX = new double[4];
    private final double[] lineVertY = new double[4];
    private final double[] arcVertX = new double[(ARC_STEPS + 1) * 2];
    private final double[] arcVertY = new double[(ARC_STEPS + 1) * 2];
    private final double[] crossingsBuffer = new double[256];

    public RadialMenuScreen() {
        cachedTitle = Component.translatable("wegui.radial.title").getString();
        cachedEmptyHint = Component.translatable("wegui.radial.empty").getString();
        this.setTitle(cachedTitle);
    }

    @Override
    public void initGui() {
        super.initGui();

        commands.clear();
        List<String> favIds = CommandHistory.getFavorites();
        for (String id : favIds) {
            Command cmd = WeCommands.get(id);
            if (cmd != null && !cmd.usages().isEmpty()) {
                commands.add(cmd);
            }
        }

        totalPages = Math.max(1, (int) Math.ceil((double) commands.size() / MAX_PER_PAGE));
        if (currentPage >= totalPages) currentPage = 0;

        Minecraft mc = Minecraft.getInstance();
        double mx = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
        centerX = (int) mx;
        centerY = (int) my;

        outerRadius = Configs.CommandPanel.RADIAL_RADIUS.getIntegerValue();
        innerRadius = Configs.CommandPanel.RADIAL_INNER_RADIUS.getIntegerValue();

        centerX = Math.max(outerRadius + 10, Math.min(this.width - outerRadius - 10, centerX));
        centerY = Math.max(outerRadius + 20, Math.min(this.height - outerRadius - 20, centerY));

        // 预计算圆环顶点（外圆顺时针 + 内圆逆时针），避免每帧重复 cos/sin
        precomputeRingVertices(outerRingVertX, outerRingVertY, outerRadius - 1, outerRadius + 1);
        precomputeRingVertices(innerRingVertX, innerRingVertY, innerRadius - 1, innerRadius + 1);
        precomputeFilledCircleVertices(innerFilledVertX, innerFilledVertY, innerRadius);
    }

    /** 预计算圆环多边形顶点到指定数组 */
    private void precomputeRingVertices(double[] vx, double[] vy, int r1, int r2) {
        int idx = 0;
        for (int i = 0; i < RING_STEPS; i++) {
            double a = 2 * Math.PI * i / RING_STEPS;
            vx[idx] = r2 * Math.cos(a);
            vy[idx] = r2 * Math.sin(a);
            idx++;
        }
        for (int i = 0; i < RING_STEPS; i++) {
            double a = 2 * Math.PI * (RING_STEPS - i) / RING_STEPS;
            vx[idx] = r1 * Math.cos(a);
            vy[idx] = r1 * Math.sin(a);
            idx++;
        }
    }

    /** 预计算实心圆多边形顶点（单圆，RING_STEPS 个顶点） */
    private void precomputeFilledCircleVertices(double[] vx, double[] vy, int radius) {
        for (int i = 0; i < RING_STEPS; i++) {
            double a = 2 * Math.PI * i / RING_STEPS;
            vx[i] = radius * Math.cos(a);
            vy[i] = radius * Math.sin(a);
        }
    }

    @Override
    public void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY) {
        // 透明背景
    }

    /** 获取当前页的命令列表 */
    private List<Command> getPageCommands() {
        int start = currentPage * MAX_PER_PAGE;
        int end = Math.min(start + MAX_PER_PAGE, commands.size());
        if (start >= commands.size()) return List.of();
        return commands.subList(start, end);
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTick) {
        List<Command> pageCmds = getPageCommands();
        hoveredSegment = getHoveredSegment(mouseX, mouseY, pageCmds);

        if (commands.isEmpty()) {
            String msg = cachedEmptyHint;
            int textWidth = this.font.width(msg);
            RenderUtils.drawOutlinedBox(ctx, centerX - textWidth / 2 - 10, centerY - 14, textWidth + 20, 28, COLOR_PAGE_BG, COLOR_DIVIDER);
            ctx.drawString(this.font, msg, centerX - textWidth / 2, centerY - 4, COLOR_TEXT, false);
            return;
        }

        int n = pageCmds.size();
        double segmentAngle = 2 * Math.PI / n;
        double startOffset = -Math.PI / 2;

        // 绘制扇区（超采样抗锯齿）
        for (int i = 0; i < n; i++) {
            double a0 = startOffset + i * segmentAngle;
            double a1 = a0 + segmentAngle;
            boolean isHovered = (i == hoveredSegment);
            int color = isHovered ? COLOR_HOVER : (i % 2 == 0 ? COLOR_SEGMENT_A : COLOR_SEGMENT_B);
            drawPieSegmentAA(ctx, centerX, centerY, innerRadius, outerRadius, a0, a1, color);
        }

        // 绘制内圆（实心 + 边缘）
        drawFilledCircleAA(ctx, centerX, centerY, innerRadius, COLOR_INNER);
        drawCircleOutlineAA(ctx, centerX, centerY, innerRadius, COLOR_INNER_RING);

        // 绘制外圆边缘
        drawCircleOutlineAA(ctx, centerX, centerY, outerRadius, COLOR_OUTER_RING);

        // 高亮选中扇区的边缘
        if (hoveredSegment >= 0 && hoveredSegment < n) {
            double a0 = startOffset + hoveredSegment * segmentAngle;
            double a1 = a0 + segmentAngle;
            drawArcEdgeAA(ctx, centerX, centerY, outerRadius, a0, a1, COLOR_HOVER_EDGE);
            drawArcEdgeAA(ctx, centerX, centerY, innerRadius, a0, a1, COLOR_HOVER_EDGE);
            drawRadialLineAA(ctx, centerX, centerY, innerRadius, outerRadius, a0, COLOR_HOVER_EDGE);
            drawRadialLineAA(ctx, centerX, centerY, innerRadius, outerRadius, a1, COLOR_HOVER_EDGE);
        }

        // 绘制扇区分割线（径向）
        for (int i = 0; i < n; i++) {
            double a = startOffset + i * segmentAngle;
            drawRadialLineAA(ctx, centerX, centerY, innerRadius, outerRadius, a, COLOR_DIVIDER);
        }

        // 绘制命令名称
        for (int i = 0; i < n; i++) {
            double a0 = startOffset + i * segmentAngle;
            double a1 = a0 + segmentAngle;
            double midAngle = (a0 + a1) / 2;
            double textR = (innerRadius + outerRadius) / 2.0;
            int tx = (int) (centerX + Math.cos(midAngle) * textR);
            int ty = (int) (centerY + Math.sin(midAngle) * textR);

            Command cmd = pageCmds.get(i);
            String name = cmd.displayName();
            int maxW = (int) (segmentAngle * textR * 0.85);
            if (maxW < 20) maxW = 20;
            if (this.font.width(name) > maxW) {
                name = this.font.plainSubstrByWidth(name, maxW - 6) + "...";
            }
            int textWidth = this.font.width(name);
            int color = (i == hoveredSegment) ? COLOR_TEXT : COLOR_TEXT_DIM;
            ctx.drawString(this.font, name, tx - textWidth / 2, ty - 4, color, false);
        }

        // 中心区域：显示当前 hover 命令的用法模板
        if (hoveredSegment >= 0 && hoveredSegment < n) {
            Command cmd = pageCmds.get(hoveredSegment);
            String text = null;
            if (!cmd.usages().isEmpty()) {
                text = cmd.usages().get(0).baseCommand();
            }
            if (text == null || text.isEmpty()) {
                text = cmd.id();
            }
            int maxW = innerRadius * 2 - 8;
            if (this.font.width(text) > maxW) {
                text = this.font.plainSubstrByWidth(text, maxW) + "...";
            }
            int tw = this.font.width(text);
            ctx.drawString(this.font, text, centerX - tw / 2, centerY - 4, COLOR_TEXT, false);
        } else {
            String title = cachedTitle;
            int tw = this.font.width(title);
            ctx.drawString(this.font, title, centerX - tw / 2, centerY - 4, COLOR_TEXT_DIM, false);
        }

        // 页数指示器
        if (totalPages > 1) {
            String pageStr = (currentPage + 1) + " / " + totalPages;
            int pw = this.font.width(pageStr);
            int px = centerX - pw / 2;
            int py = centerY + outerRadius + 10;
            RenderUtils.drawOutlinedBox(ctx, px - 8, py - 3, pw + 16, 16, COLOR_PAGE_BG, COLOR_PAGE_EDGE);
            ctx.drawString(this.font, pageStr, px, py + 1, COLOR_PAGE_TEXT, false);
        }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.onMouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            List<Command> pageCmds = getPageCommands();
            int idx = getHoveredSegment((int) event.x(), (int) event.y(), pageCmds);
            if (idx >= 0 && idx < pageCmds.size()) {
                executeCommand(pageCmds.get(idx));
                return true;
            }
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.mc.setScreenAndShow(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (totalPages <= 1) return false;
        long now = System.currentTimeMillis();
        if (now - lastScrollTime < SCROLL_THROTTLE_MS) {
            return true; // 节流期内消费事件但不切页
        }
        lastScrollTime = now;
        if (verticalAmount < 0) {
            currentPage = (currentPage + 1) % totalPages;
        } else {
            currentPage = (currentPage - 1 + totalPages) % totalPages;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.mc.setScreenAndShow(null);
            return true;
        }
        return super.keyPressed(event);
    }

    private void executeCommand(Command cmd) {
        if (cmd.usages().isEmpty()) return;
        Usage usage = cmd.usages().get(0);
        Minecraft mc = Minecraft.getInstance();

        if (cmd.usages().size() == 1 && usage.type() == WeCommands.Type.INSTANT) {
            CommandSender.send(usage.baseCommand());
            CommandHistory.recordRecent(cmd.id());
            mc.setScreenAndShow(null);
        } else {
            mc.setScreenAndShow(new ParamInputScreen(null, cmd, usage));
        }
    }

    private int getHoveredSegment(int mouseX, int mouseY, List<Command> pageCmds) {
        if (pageCmds.isEmpty()) return -1;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < innerRadius || dist > outerRadius + 10) return -1;

        double angle = Math.atan2(dy, dx);
        double normalized = (angle + Math.PI / 2 + 2 * Math.PI) % (2 * Math.PI);
        double segmentAngle = 2 * Math.PI / pageCmds.size();
        int idx = (int) (normalized / segmentAngle);
        if (idx >= pageCmds.size()) idx = pageCmds.size() - 1;
        return idx;
    }

    // ==================== 渲染方法（多边形扫描线填充） ====================

    /**
     * 绘制扇区段。构建扇区多边形（外圆弧 + 内圆弧），用 fillPolygon 填充。
     * 使用实例字段 sectorVertX/Y 避免数组分配。
     */
    private void drawPieSegmentAA(GuiContext ctx, int cx, int cy, int innerR, int outerR,
                                  double startAngle, double endAngle, int color) {
        int totalVerts = (ARC_STEPS + 1) * 2;
        int idx = 0;

        // 外圆弧（startAngle → endAngle）
        for (int i = 0; i <= ARC_STEPS; i++) {
            double a = startAngle + (endAngle - startAngle) * i / ARC_STEPS;
            sectorVertX[idx] = outerR * Math.cos(a);
            sectorVertY[idx] = outerR * Math.sin(a);
            idx++;
        }
        // 内圆弧（endAngle → startAngle，反向闭合）
        for (int i = 0; i <= ARC_STEPS; i++) {
            double a = endAngle - (endAngle - startAngle) * i / ARC_STEPS;
            sectorVertX[idx] = innerR * Math.cos(a);
            sectorVertY[idx] = innerR * Math.sin(a);
            idx++;
        }

        fillPolygon(ctx, cx, cy, sectorVertX, sectorVertY, totalVerts, color);
    }

    /** 填充圆 */
    private void drawFilledCircleAA(GuiContext ctx, int cx, int cy, int radius, int color) {
        fillPolygon(ctx, cx, cy, innerFilledVertX, innerFilledVertY, innerFilledVertX.length, color);
    }

    /** 圆形描边：使用预计算的圆环顶点 */
    private void drawCircleOutlineAA(GuiContext ctx, int cx, int cy, int radius, int color) {
        double[] vx = (radius == outerRadius) ? outerRingVertX : innerRingVertX;
        double[] vy = (radius == outerRadius) ? outerRingVertY : innerRingVertY;
        fillPolygon(ctx, cx, cy, vx, vy, vx.length, color);
    }

    /**
     * 绘制弧线边缘：构建完整弧形带状多边形（外弧 + 内弧），用 fillPolygon 填充。
     * 消除分段矩形的间隙问题。
     */
    private void drawArcEdgeAA(GuiContext ctx, int cx, int cy, int radius,
                               double startAngle, double endAngle, int color) {
        int totalVerts = (ARC_STEPS + 1) * 2;
        int idx = 0;
        int r1 = radius - 1, r2 = radius + 1;

        // 外弧（startAngle → endAngle）
        for (int i = 0; i <= ARC_STEPS; i++) {
            double a = startAngle + (endAngle - startAngle) * i / ARC_STEPS;
            arcVertX[idx] = r2 * Math.cos(a);
            arcVertY[idx] = r2 * Math.sin(a);
            idx++;
        }
        // 内弧（endAngle → startAngle，反向闭合）
        for (int i = 0; i <= ARC_STEPS; i++) {
            double a = endAngle - (endAngle - startAngle) * i / ARC_STEPS;
            arcVertX[idx] = r1 * Math.cos(a);
            arcVertY[idx] = r1 * Math.sin(a);
            idx++;
        }

        fillPolygon(ctx, cx, cy, arcVertX, arcVertY, totalVerts, color);
    }

    /** 径向线 */
    private void drawRadialLineAA(GuiContext ctx, int cx, int cy, int innerR, int outerR,
                                  double angle, int color) {
        double x1 = innerR * Math.cos(angle);
        double y1 = innerR * Math.sin(angle);
        double x2 = outerR * Math.cos(angle);
        double y2 = outerR * Math.sin(angle);
        drawLineSegment(ctx, cx, cy, x1, y1, x2, y2, color);
    }

    /** 绘制线段（扩展为宽度 1.5 的矩形条，使用实例字段数组） */
    private void drawLineSegment(GuiContext ctx, int cx, int cy,
                                 double x1, double y1, double x2, double y2, int color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001) return;
        double nx = -dy / len * 0.75;
        double ny = dx / len * 0.75;

        lineVertX[0] = x1 + nx; lineVertY[0] = y1 + ny;
        lineVertX[1] = x2 + nx; lineVertY[1] = y2 + ny;
        lineVertX[2] = x2 - nx; lineVertY[2] = y2 - ny;
        lineVertX[3] = x1 - nx; lineVertY[3] = y1 - ny;

        fillPolygon(ctx, cx, cy, lineVertX, lineVertY, 4, color);
    }

    /**
     * 多边形扫描线填充。对每行计算多边形边与水平线的交点，配对后用 fill 绘制。
     * 使用实例字段 crossingsBuffer 避免每次分配。
     */
    private void fillPolygon(GuiContext ctx, int cx, int cy,
                             double[] vertX, double[] vertY, int nVerts, int color) {
        double yMin = vertY[0], yMax = vertY[0];
        for (int i = 1; i < nVerts; i++) {
            if (vertY[i] < yMin) yMin = vertY[i];
            if (vertY[i] > yMax) yMax = vertY[i];
        }

        int yStart = (int) Math.floor(yMin);
        int yEnd = (int) Math.ceil(yMax);

        for (int y = yStart; y <= yEnd; y++) {
            double yc = y + 0.5;
            int crossingCount = 0;

            for (int i = 0; i < nVerts; i++) {
                int j = (i + 1) % nVerts;
                double y1 = vertY[i], y2 = vertY[j];
                if ((y1 <= yc && y2 > yc) || (y2 <= yc && y1 > yc)) {
                    double t = (yc - y1) / (y2 - y1);
                    if (crossingCount >= crossingsBuffer.length) break;
                    crossingsBuffer[crossingCount++] = vertX[i] + t * (vertX[j] - vertX[i]);
                }
            }

            // 排序交点（少量元素，插入排序）
            for (int i = 1; i < crossingCount; i++) {
                double val = crossingsBuffer[i];
                int j = i - 1;
                while (j >= 0 && crossingsBuffer[j] > val) {
                    crossingsBuffer[j + 1] = crossingsBuffer[j];
                    j--;
                }
                crossingsBuffer[j + 1] = val;
            }

            for (int i = 0; i + 1 < crossingCount; i += 2) {
                int x1 = (int) Math.round(crossingsBuffer[i]);
                int x2 = (int) Math.round(crossingsBuffer[i + 1]);
                if (x2 < x1) { int t = x1; x1 = x2; x2 = t; }
                ctx.fill(cx + x1, cy + y, cx + x2 + 1, cy + y + 1, color);
            }
        }
    }
}
