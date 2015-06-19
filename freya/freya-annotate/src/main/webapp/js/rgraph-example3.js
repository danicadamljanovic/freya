var Log = {
	elem : false,
	write : function(text) {
		if (!this.elem)
			this.elem = document.getElementById('log');
		this.elem.innerHTML = text;
		this.elem.style.left = (500 - this.elem.offsetWidth / 2) + 'px';
	}
};

function addEvent(obj, type, fn) {
	if (obj.addEventListener)
		obj.addEventListener(type, fn, false);
	else
		obj.attachEvent('on' + type, fn);
};

function init(result) {
	var infovis = document.getElementById('infovis');
	var w = infovis.offsetWidth, h = infovis.offsetHeight;
	var json = result;
	// init data

	var graph = '[{id:"190_0", adjacencies:["node0"]}, {id:"node0", name:"node0 name", data:{$dim:8.660354683365695, "some other key":"some other value"}, adjacencies:["node1", "node2", "node3", "node4", "node5"]}, {id:"node1", name:"node1 name", data:{$dim:21.118129724156983, "some other key":"some other value"}, adjacencies:["node0", "node2", "node3", "node4", "node5"]}, {id:"node2", name:"node2 name", data:{$dim:6.688951018413683, "some other key":"some other value"}, adjacencies:["node0", "node1", "node3", "node4", "node5"]}, {id:"node3", name:"node3 name", data:{$dim:19.78771599710248, "some other key":"some other value"}, adjacencies:["node0", "node1", "node2", "node4", "node5"]}, {id:"node4", name:"node4 name", data:{$dim:3.025781742947326, "some other key":"some other value"}, adjacencies:["node0", "node1", "node2", "node3", "node5"]}, {id:"node5", name:"node5 name", data:{$dim:9.654383829711456, "some other key":"some other value"}, adjacencies:["node0", "node1", "node2", "node3", "node4"]}, {id:"4619_46", adjacencies:["190_0"]}, {id:"236585_30", adjacencies:["190_0"]}, {id:"131161_18", adjacencies:["190_0"]}, {id:"41529_12", adjacencies:["190_0"]}]';
	// end

	// init canvas
	// Create a new canvas instance.
	init.canvas = init.canvas || new Canvas("mycanvas", {
		'injectInto' : 'infovis',
		'width' : w,
		'height' : h,
		// Optional: Create a background canvas
		// for painting concentric circles.
		'backgroundCanvas' : {
			'styles' : {
				'strokeStyle' : '#444'
			},
			'impl' : {
				'init' : function() {
				},
				'plot' : function(canvas, ctx) {
					var times = 6, d = 150;
					var pi2 = Math.PI * 2;
					for ( var i = 1; i <= times; i++) {
						ctx.beginPath();
						ctx.arc(0, 0, i * d, 0, pi2, true);
						ctx.stroke();
						ctx.closePath();
					}
				}
			}
		}
	});

	document.getElementById('mycanvas-label').innerHTML = '';
	document.getElementById('right-container').innerHTML = '';
	RGraph.Plot.EdgeTypes.implement( {
		'custom-line' : function(adj, canvas) {
			// plot arrow edge
			this.edgeTypes.arrow.call(this, adj, canvas);
			// get nodes cartesian coordinates
			var pos = adj.nodeFrom.pos.getc(true);
			var posChild = adj.nodeTo.pos.getc(true);
			// check for edge label in data
			var data = adj.data;
			if (data.labelid && data.labeltext) {
				var domlabel = document.getElementById(data.labelid);
				// if the label doesn't exist create it and append it
				// to the label container
				if (!domlabel) {
					domlabel = document.createElement('div');
					domlabel.id = data.labelid;
					domlabel.innerHTML = data.labeltext;
					// add some custom style
					var style = domlabel.style;
					style.position = 'absolute';
					style.color = '#fff';
					style.fontSize = '7px';
					// append the label to the labelcontainer
					this.getLabelContainer().appendChild(domlabel);
				}

				// now adjust the label placement
				var radius = this.viz.canvas.getSize();
				domlabel.style.left = parseInt((pos.x + posChild.x
						+ radius.width - domlabel.offsetWidth) / 2) + 'px';
				domlabel.style.top = parseInt((pos.y + posChild.y + radius.height) / 2) + 'px';
			}
		}
	});
	// init RGraph
	var rgraph = new RGraph(init.canvas, {
		interpolation : 'polar',
		levelDistance : 150,
		// Set Edge and Node colors.
		Node : {
			color : '#ccddee'
		},

		// Edge: {
		// color: '#772277'
		// },
		Edge : {
			'overridable' : true,
			'color' : '#cccc00',
			'type' : 'custom-line'
		},
		// Add the node's name into the label
		// This method is called only once, on label creation.
		onCreateLabel : function(domElement, node) {
			domElement.innerHTML = node.name;
			domElement.style.cursor = "pointer";
			domElement.onclick = function() {
				 rgraph.onClick(node.id, {
				 hideLabels : false
				 });
			};
		},

		// Change the node's style based on its position.
		// This method is called each time a label is rendered/positioned
		// during an animation.
		onPlaceLabel : function(domElement, node) {
			var style = domElement.style;
			style.display = '';

			var mainSubject = node.data.mainSubject;
			//alert(mainSubject);
			if (mainSubject == true){
				 rgraph.onClick(node.id, {
				 hideLabels : false
				 });
			};
			
//			if (node._depth <= 1) {
//				style.fontSize = "0.8em";
//				style.color = "#ccc";
//
//			} else if (node._depth == 2) {
//				style.fontSize = "0.7em";
//				style.color = "#494949";
//
//			} else {
//				style.display = 'none';
//			}

			var left = parseInt(style.left);
			var w = domElement.offsetWidth;
			style.left = (left - w / 2) + 'px';
		},

		onAfterCompute : function() {
			Log.write("done");

			// Make the relations list shown in the right column.
		var node = Graph.Util.getClosestNodeToOrigin(rgraph.graph, "pos");

		var answer = node.data.answer;
		var header=node.data.header;
		if (header==null)
			header=node.name;
		//alert("rendering right pane...header:"+header+" answer:"+answer);
		var html = "<h4>" + header + "</h4>";
		// var tempTxt = html.replace(/\+/g, ' ');
		html = html + "<ul>"
		var ch = node.data.answer;
		if (ch && ch.length > 0) {
			for ( var i = 0; i < ch.length; i++) {
				html = html + "<li>"
				html = html + ch[i];
				html = html + "</li>"
			}
		}
		html = html + "</ul>"
		document.getElementById('right-container').innerHTML = (html);
	}
	});
	// load JSON data.
	//rgraph.loadJSON(eval('(' + json + ')'));
	rgraph.loadJSON(json);
	// add some extra edges to the tree
	// to make it a graph (just for fun)
	/*
	 * rgraph.graph.addAdjacence({ 'id': '236585_30' }, { 'id': '236583_23' },
	 * null); rgraph.graph.addAdjacence({ 'id': '236585_30' }, { 'id': '4619_46' },
	 * null);
	 */
	// Compute positions and plot
	rgraph.refresh();
	// end

	// Global Options
	// Define a function that returns the selected duration
	function getDuration() {
		var sduration = document.getElementById('select-duration');
		var sdindex = sduration.selectedIndex;
		return parseInt(sduration.options[sdindex].text);
	}
	;
	// Define a function that returns the selected fps
	function getFPS() {
		var fpstype = document.getElementById('select-fps');
		var fpsindex = fpstype.selectedIndex;
		return parseInt(fpstype.options[fpsindex].text);
	}
	;
	// Define a function that returns whether you have to
	// hide labels during the animation or not.
	function hideLabels() {
		return document.getElementById('hide-labels').checked;
	}
	;

	// init handlers
	// Add event handlers to the right column controls.

	// Remove Nodes
	var button = document.getElementById('remove-nodes');
//	button.onclick = function() {
//		// get animation type.
//		var stype = document.getElementById('select-type-remove-nodes');
//		var sindex = stype.selectedIndex;
//		var type = stype.options[sindex].text;
//		// get node ids to be removed.
//		var subnodes = Graph.Util.getSubnodes(rgraph.graph.getNode('236797_5'),
//				0);
//		var map = [];
//		for ( var i = 0; i < subnodes.length; i++) {
//			map.push(subnodes[i].id);
//		}
//		// perform node-removing animation.
//		rgraph.op.removeNode(map.reverse(), {
//			type : type,
//			duration : getDuration(),
//			fps : getFPS(),
//			hideLabels : hideLabels()
//		});
//	};

	// Remove edges
	button = document.getElementById('remove-edges');
//	button.onclick = function() {
//		// get animation type.
//		var stype = document.getElementById('select-type-remove-edges');
//		var sindex = stype.selectedIndex;
//		var type = stype.options[sindex].text;
//		// perform edge removing animation.
//		rgraph.op.removeEdge( [ [ '236585_30', "190_0" ],
//				[ '236585_30', '4619_46' ] ], {
//			type : type,
//			duration : getDuration(),
//			fps : getFPS(),
//			hideLabels : hideLabels()
//		});
//	};

	// Add a Graph (Sum)
	button = document.getElementById('sum');
//	button.onclick = function() {
//		// get graph to add.
//		var trueGraph = eval('(' + graph + ')');
//		// get animation type.
//		var stype = document.getElementById('select-type-sum');
//		var sindex = stype.selectedIndex;
//		var type = stype.options[sindex].text;
//		// perform sum animation.
//		rgraph.op.sum(trueGraph, {
//			type : type,
//			fps : getFPS(),
//			duration : getDuration(),
//			hideLabels : hideLabels(),
//			onComplete : function() {
//				Log.write("sum complete!");
//			}
//		});
//	};

	// Morph
	button = document.getElementById('morph');
//	button.onclick = function() {
//		// get graph to morph to.
//		var trueGraph = eval('(' + graph + ')');
//		// get animation type.
//		var stype = document.getElementById('select-type-morph');
//		var sindex = stype.selectedIndex;
//		var type = stype.options[sindex].text;
//		// perform morphing animation.
//		rgraph.op.morph(trueGraph, {
//			type : type,
//			fps : getFPS(),
//			duration : getDuration(),
//			hideLabels : hideLabels(),
//			onComplete : function() {
//				Log.write("morph complete!");
//			}
//		});
//	};
	// end
}
