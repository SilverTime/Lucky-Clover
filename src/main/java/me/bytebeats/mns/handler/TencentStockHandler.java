package me.bytebeats.mns.handler;

import me.bytebeats.mns.listener.MousePressedListener;
import me.bytebeats.mns.network.HttpClientPool;
import me.bytebeats.mns.tool.MarketTimeUtils;
import me.bytebeats.mns.tool.NotificationUtil;
import me.bytebeats.mns.meta.Stock;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TencentStockHandler extends AbsStockHandler {

    // 标记是否已经请求过一次数据（用于非开市时间只请求一次）
    private boolean hasRequestedOnce = false;
    // 当前市场类型
    private MarketType currentMarketType = MarketType.ALL;

    public enum MarketType {
        CN,    // A股
        HK,    // 港股
        US,    // 美股
        ALL    // 全部市场
    }

    public TencentStockHandler(JTable table, JLabel label) {
        super(table, label);
        table.addMouseListener(new MousePressedListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                int selectedRowIdx = jTable.getSelectedRow();
                if (selectedRowIdx < 0) {
                    return;
                }
                String symbol = stocks.get(selectedRowIdx).getSymbol();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2 && onItemDoubleClickListener != null) {
                        onItemDoubleClickListener.onItemDoubleClick(symbol, e.getXOnScreen(), e.getYOnScreen());
                    } else if (e.getClickCount() == 1 && onItemClickListener != null) {
                        onItemClickListener.onItemClick(symbol, e.getXOnScreen(), e.getYOnScreen());
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    if (onItemRightClickListener != null) {
                        onItemRightClickListener.onItemRightClick(symbol, e.getXOnScreen(), e.getYOnScreen());
                    }
                }
            }
        });

    }

    @Override
    public String[] getColumnNames() {
        return handleColumnNames(stockColumnNames);
    }

    @Override
    public void load(List<String> symbols) {
        stocks.clear();
        // 重置请求标志
        hasRequestedOnce = false;
        
        if (timer == null) {
            timer = new Timer();
            updateFrequency();
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                fetch(symbols);
            }
        }, 0, frequency);
        NotificationUtil.info("starts updating " + getTipText() + " stocks");
    }

    /**
     * 设置当前市场类型
     */
    public void setMarketType(MarketType marketType) {
        this.currentMarketType = marketType;
    }

    @Override
    protected String getTipText() {
        return jTable.getToolTipText();
    }

    private void fetch(List<String> symbols) {
        if (symbols.isEmpty()) {
            return;
        }

        // 检查市场是否开市
        boolean isMarketOpen = isCurrentMarketOpen();
        
        // 如果市场未开市且已经请求过一次数据，则停止定时器
        if (!isMarketOpen && hasRequestedOnce) {
            stop();
            NotificationUtil.info(getTipText() + " market is closed, stops updating");
            return;
        }

        StringBuilder params = new StringBuilder();
        for (String symbol : symbols) {
            if (params.length() != 0) {
                params.append(',');
            }
            params.append(symbol);
        }
        try {
            String entity = HttpClientPool.getInstance().get(appendParams(params.toString()));
            parse(symbols, entity);
            
            // 标记已经请求过一次数据
            hasRequestedOnce = true;
            
            // 如果市场未开市，请求一次后立即停止
            if (!isMarketOpen) {
                stop();
                NotificationUtil.info(getTipText() + " market is closed, fetched data once");
            }
        } catch (Exception e) {
            NotificationUtil.info(e.getMessage());
            timer.cancel();
            timer = null;
            NotificationUtil.info("stops updating " + jTable.getToolTipText() + " data because of " + e.getMessage());
        }
    }

    /**
     * 判断当前选择的市场是否开市
     */
    private boolean isCurrentMarketOpen() {
        switch (currentMarketType) {
            case CN:
                return MarketTimeUtils.isCNMarketOpen();
            case HK:
                return MarketTimeUtils.isHKMarketOpen();
            case US:
                return MarketTimeUtils.isUSMarketOpen();
            case ALL:
            default:
                // 全部市场：任一市场开市就继续刷新
                return MarketTimeUtils.isCNMarketOpen() || MarketTimeUtils.isHKMarketOpen() || MarketTimeUtils.isUSMarketOpen();
        }
    }
    
    @Override
    protected String getMarketStatusSuffix() {
        boolean isOpen = isCurrentMarketOpen();
        if (isOpen) {
            return "";
        } else {
            return " (已休市)";
        }
    }

    private void parse(List<String> symbols, String entity) {
        String[] raws = entity.split("\n");
        if (symbols.size() != raws.length) {
            return;
        }
        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);
            String raw = raws[i];
            String assertion = String.format("(?<=v_%s=\").*?(?=\";)", symbol);
            Pattern pattern = Pattern.compile(assertion);
            Matcher matcher = pattern.matcher(raw);
            while (matcher.find()) {
                String[] metas = matcher.group().split("~");
                Stock stock = new Stock();
//                stock.setSymbol(symbol);
//                stock.setName(metas[1]);
//                stock.setLatestPrice(Double.parseDouble(metas[3]));
//                stock.setChange(Double.parseDouble(metas[31]));
//                stock.setChangeRatio(Double.parseDouble(metas[32]));
//                stock.setVolume(Double.parseDouble(metas[36]));
//                stock.setTurnover(Double.parseDouble(metas[37]));
//                stock.setMarketValue(Double.parseDouble(metas[45]));
                //简要信息
                stock.setSymbol(symbol);
                stock.setName(metas[1]);
                stock.setLatestPrice(Double.parseDouble(metas[3]));
                stock.setChange(Double.parseDouble(metas[4]));
                stock.setChangeRatio(Double.parseDouble(metas[5]));
                updateStock(stock);
                updateView();
            }
        }
    }
}
