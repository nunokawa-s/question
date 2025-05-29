// public/js/script.js

// DOMContentLoaded イベントリスナーで、HTML要素が完全に読み込まれてからスクリプトを実行します。
// これにより、要素が見つからないエラー（Cannot read properties of null）を防ぎます。
document.addEventListener('DOMContentLoaded', function() {
    // 'tabcontrol' と 'tabbody' の要素を取得します。
    // これらの要素がHTML内に存在することを確認してください。
    var tabControlElement = document.getElementById('tabcontrol');
    var tabBodyElement = document.getElementById('tabbody');

    // 要素がnullでないことを確認してから処理を進めます。
    if (!tabControlElement || !tabBodyElement) {
        console.error("エラー: 'tabcontrol' または 'tabbody' 要素がHTMLに見つかりません。");
        return; // 要素が見つからない場合は、これ以上処理を続行しません。
    }

    var tabs = tabControlElement.getElementsByTagName('a');
    var pages = tabBodyElement.getElementsByTagName('div');

    // タブの切り替え処理を定義します。
    function changeTab() {
        // クリックされたタブのhref属性から、対応するページのIDを抽出します。
        var targetid = this.href.substring(this.href.indexOf('#') + 1, this.href.length);

        // すべてのタブページを非表示にし、対象のページだけを表示します。
        for (var i = 0; i < pages.length; i++) {
            if (pages[i].id !== targetid) {
                pages[i].style.display = "none";
            } else {
                pages[i].style.display = "block";
            }
        }

        // すべてのタブのz-indexをリセットし、クリックされたタブを前面に表示します。
        for (var i = 0; i < tabs.length; i++) {
            tabs[i].style.zIndex = "0";
        }
        this.style.zIndex = "10";

        // リンクのデフォルトのページ遷移を防ぎます。
        return false;
    }

    // すべてのタブ（a要素）にクリックイベントリスナーを設定します。
    for (var i = 0; i < tabs.length; i++) {
        tabs[i].onclick = changeTab;
    }

    // ページが読み込まれた際に、最初のタブをアクティブにします。
    // tabs[0] が存在することを確認してから呼び出します。
    if (tabs.length > 0) {
        tabs[0].onclick();
    } else {
        console.warn("警告: 'tabcontrol' 内にタブ（a要素）が見つかりません。");
    }
});
