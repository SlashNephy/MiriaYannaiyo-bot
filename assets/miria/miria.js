!function() {
    const results = document.getElementById("miria-result");

    class ElementBuilder {
        constructor(data, index) {
            this.data = data;
            this.index = index;

            this.card = document.createElement("div");
            results.appendChild(this.card);
            this.card.classList.add("card");

            this.originalStatusButton = null;
            this.featuresButton = null;
            this.rejectedFeaturesButton = null;
        }

        createHeader() {
            const header = document.createElement("div");
            this.card.appendChild(header);
            header.classList.add("card-header");

            const time = document.createElement("span");
            header.appendChild(time);
            time.classList.add("badge", "badge-secondary");
            time.innerText = (new Date(this.data.time)).toLocaleString();

            const tweet = document.createElement("span");
            header.appendChild(tweet);
            tweet.innerText = this.data.status.text;
        }

        createBody() {
            const body = document.createElement("div");
            this.card.appendChild(body);
            body.classList.add("card-body");

            const center = document.createElement("center");
            body.appendChild(center);
            const blockQuote = document.createElement("blockquote");
            center.appendChild(blockQuote);
            const p = document.createElement("p");
            blockQuote.appendChild(p);
            p.innerText = this.data.status.text;

            const description = document.createElement("p");
            body.appendChild(description);
            description.innerHTML = `単語の候補は ${Object.keys(this.data.candidates).map(e => `「${e}」`).join(", ")} でした。元のツイートは <b>${this.data.sourceStatus.via}</b> から投稿されました。パターン係数は r = <code>${this.data.pattern.r.toPrecision(4)}</code> でした。`;

            const buttons = document.createElement("div");
            body.appendChild(buttons);
            buttons.classList.add("right");

            this.originalStatusButton = document.createElement("a");
            buttons.appendChild(this.originalStatusButton);
            this.originalStatusButton.classList.add("btn", "btn-success", "btn-sm", "original-trigger");
            this.originalStatusButton.setAttribute("role", "button");
            this.originalStatusButton.setAttribute("data-toggle", "collapse");
            this.originalStatusButton.href = `#original-status-${this.index}`;
            this.originalStatusButton.innerText = "原文ツイートを表示";

            this.featuresButton = document.createElement("a");
            buttons.appendChild(this.featuresButton);
            this.featuresButton.classList.add("btn", "btn-info", "btn-sm", "feature-trigger");
            this.featuresButton.setAttribute("role", "button");
            this.featuresButton.setAttribute("data-toggle", "collapse");
            this.featuresButton.href = `#features-${this.index}`;
            this.featuresButton.innerText = "品詞情報を展開";

            this.rejectedFeaturesButton = document.createElement("a");
            buttons.appendChild(this.rejectedFeaturesButton);
            this.rejectedFeaturesButton.classList.add("btn", "btn-danger", "btn-sm", "rejected-feature-trigger");
            this.rejectedFeaturesButton.setAttribute("role", "button");
            this.rejectedFeaturesButton.setAttribute("data-toggle", "collapse");
            this.rejectedFeaturesButton.href = `#rejected-features-${this.index}`;
            this.rejectedFeaturesButton.innerText = "リジェクトされた品詞情報を展開";
        }

        createOriginalStatusCard() {
            const card = document.createElement("div");
            this.card.appendChild(card);
            card.id = `original-status-${this.index}`;
            card.classList.add("card-collapse", "collapse", "out");

            const body = document.createElement("div");
            card.appendChild(body);
            body.classList.add("card-body");

            const hr = document.createElement("hr");
            body.appendChild(hr);

            const p = document.createElement("p");
            p.innerHTML = '<i class="fab fa-twitter"></i> 原文ツイート';

            const center = document.createElement("center");
            body.appendChild(center);
            const blockQuote = document.createElement("blockquote");
            center.appendChild(blockQuote);
            const p2 = document.createElement("p");
            blockQuote.appendChild(p2);
            p2.innerText = this.data.sourceStatus.text;
            const p3 = document.createElement("p");
            blockQuote.appendChild(p3);
            p3.innerText = `${this.data.sourceStatus.author.name} (@${this.data.sourceStatus.author.screenName})`;
        }

        createFeaturesCard() {
            const card = document.createElement("div");
            this.card.appendChild(card);
            card.id = `features-${this.index}`;
            card.classList.add("card-collapse", "collapse", "out");

            const body = document.createElement("div");
            card.appendChild(body);
            body.classList.add("card-body");

            const hr = document.createElement("hr");
            body.appendChild(hr);

            const p = document.createElement("p");
            body.appendChild(p);
            p.innerHTML = '<i class="far fa-list-alt"></i> 品詞情報テーブル';

            const table = document.createElement("table");
            body.appendChild(table);
            table.classList.add("table");

            this.createFeaturesTable(table, this.data.nodes);
        }

        createRejectedFeaturesCard() {
            const card = document.createElement("div");
            this.card.appendChild(card);
            card.id = `rejected-features-${this.index}`;
            card.classList.add("card-collapse", "collapse", "out");

            const body = document.createElement("div");
            card.appendChild(body);
            body.classList.add("card-body");

            const hr = document.createElement("hr");
            body.appendChild(hr);

            const p = document.createElement("p");
            body.appendChild(p);
            p.innerHTML = '<i class="far fa-list-alt"></i> リジェクトされた品詞情報テーブル';

            const p2 = document.createElement("p");
            body.appendChild(p2);
            p2.innerText = "感情分析の結果, ネガティブが圧倒する場合はリジェクトされます。";

            const table = document.createElement("table");
            body.appendChild(table);
            table.classList.add("table");

            this.createFeaturesTable(table, this.data.deletedNodes);
        }

        createFeaturesTable(table, nodes) {
            const thead = document.createElement("thead");
            table.appendChild(thead);

            const theadTr = document.createElement("tr");
            thead.appendChild(theadTr);
            theadTr.innerHTML = '<th>単語 (読み)</th><td>品詞</td><td>感情分析</td><td>イメージ</td>';

            const tbody = document.createElement("tbody");
            table.appendChild(tbody);

            for (const node of nodes) {
                const tr = document.createElement("tr");
                tbody.appendChild(tr);

                const wordTh = document.createElement("th");
                tr.appendChild(wordTh);
                if (node.surface !== node.reading) {
                    wordTh.innerText = `${node.surface} (${node.reading})`;
                } else {
                    wordTh.innerText = node.surface;
                }
                if (node.deleted) {
                    const span = document.createElement("span");
                    wordTh.appendChild(span);
                    span.classList.add("badge", "badge-warning");
                    span.innerText = "リジェクト";
                }

                const featureTd = document.createElement("td");
                tr.appendChild(featureTd);
                featureTd.innerText = node.feature;
                if (node.feature.includes("アイマス関連名詞")) {
                    const span = document.createElement("span");
                    featureTd.appendChild(span);
                    span.classList.add("badge", "badge-primary");
                    span.innerText = "アイマス関連名詞";

                    const p = document.createElement("p");
                    featureTd.appendChild(p);
                    p.innerText = `${node.description}`;
                }

                const feelingTd = document.createElement("td");
                tr.appendChild(feelingTd);
                if (node.feeling) {
                    const progress = document.createElement("div");
                    feelingTd.appendChild(progress);
                    progress.classList.add("progress");

                    const positiveProgressBar = document.createElement("div");
                    progress.appendChild(positiveProgressBar);
                    positiveProgressBar.classList.add("progress-bar", "progress-bar-striped", "bg-success");
                    positiveProgressBar.setAttribute("role", "progressbar");
                    positiveProgressBar.style = `width: ${node.feeling.positive}%;`;

                    const positiveSpan = document.createElement("span");
                    positiveProgressBar.appendChild(positiveSpan);
                    positiveSpan.innerText = `${node.feeling.positive} %`;

                    const neutralProgressBar = document.createElement("div");
                    progress.appendChild(neutralProgressBar);
                    neutralProgressBar.classList.add("progress-bar", "bg-empty");
                    neutralProgressBar.setAttribute("role", "progressbar");
                    neutralProgressBar.style = `width: ${node.feeling.neutral}%;`;

                    const negativeProgressBar = document.createElement("div");
                    progress.appendChild(negativeProgressBar);
                    negativeProgressBar.classList.add("progress-bar", "progress-bar-striped", "bg-danger");
                    negativeProgressBar.setAttribute("role", "progressbar");
                    negativeProgressBar.style = `width: ${node.feeling.negative}%;`;

                    const negativeSpan = document.createElement("span");
                    negativeProgressBar.appendChild(negativeSpan);
                    negativeSpan.innerText = `${node.feeling.negative} %`;
                }

                const imageTd = document.createElement("td");
                tr.appendChild(imageTd);
                if (node.feeling) {
                    switch (node.feeling.active) {
                        case "positive":
                            imageTd.innerText = "-> ";
                            const span = document.createElement("span");
                            imageTd.appendChild(span);
                            span.classList.add("plus");
                            span.innerText = "プラス";
                            break;
                        case "negative":
                            imageTd.innerText = "-> ";
                            const span2 = document.createElement("span");
                            imageTd.appendChild(span2);
                            span2.classList.add("minus");
                            span2.innerText = "マイナス";
                            break;
                        default:
                            imageTd.innerText = "-";
                    }
                }
            }
        }

        build() {
            this.createHeader();
            this.createBody();
            this.createOriginalStatusCard();
            this.createFeaturesCard();
            this.createRejectedFeaturesCard();

            new Collapse(this.originalStatusButton);
            new Collapse(this.featuresButton);
            new Collapse(this.rejectedFeaturesButton);
        }
    }

    function refresh() {
        const xhr = new XMLHttpRequest();
        xhr.open("GET", "https://api.ya.ru/v1/miria/query?count=10", true);
        xhr.onload = () => {
            if (xhr.readyState === 4 && xhr.status === 200) {
                const json = JSON.parse(xhr.responseText);
                window.json = json;

                json.forEach((t, i) => {
                    const builder = new ElementBuilder(t, i);
                    builder.build();
                });
            }
        };
        xhr.send();
    }

    window.onload = refresh;
}();
